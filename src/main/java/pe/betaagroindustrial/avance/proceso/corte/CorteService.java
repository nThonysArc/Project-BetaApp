package pe.betaagroindustrial.avance.proceso.corte;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.BusinessRuleException;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.maquina.Maquina;
import pe.betaagroindustrial.avance.maquina.MaquinaKato;
import pe.betaagroindustrial.avance.maquina.MaquinaKatoRepository;
import pe.betaagroindustrial.avance.maquina.MaquinaRepository;
import pe.betaagroindustrial.avance.proceso.ProcesoDiario;
import pe.betaagroindustrial.avance.proceso.ProcesoDiarioRepository;
import pe.betaagroindustrial.avance.proceso.auditoria.AuditoriaEdicion;
import pe.betaagroindustrial.avance.proceso.auditoria.AuditoriaEdicionRepository;
import pe.betaagroindustrial.avance.proceso.corte.dto.*;
import pe.betaagroindustrial.avance.usuario.Usuario;
import pe.betaagroindustrial.avance.usuario.UsuarioRepository;
import pe.betaagroindustrial.avance.variedad.Variedad;
import pe.betaagroindustrial.avance.variedad.VariedadRepository;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Corazon del sistema: clonado del corte anterior para armar el formulario
 * simplificado. Cada fila de detalle es maquina + kato + variedad, para
 * poder saber cuantos kilos proceso especificamente cada kato de cada
 * maquina - no solo el total agregado del corte.
 */
@Service
@RequiredArgsConstructor
public class CorteService {

    private final CorteRepository corteRepository;
    private final ProcesoDiarioRepository procesoDiarioRepository;
    private final MaquinaRepository maquinaRepository;
    private final MaquinaKatoRepository maquinaKatoRepository;
    private final VariedadRepository variedadRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaEdicionRepository auditoriaEdicionRepository;

    // ==========================================================
    // 1. PLANTILLA DEL SIGUIENTE CORTE (no persiste nada)
    // ==========================================================

    @Transactional(readOnly = true)
    public CortePlantillaResponse obtenerPlantillaSiguiente(Long procesoDiarioId) {
        ProcesoDiario proceso = procesoDiarioRepository.findById(procesoDiarioId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", procesoDiarioId));

        Optional<Corte> ultimoCorte = corteRepository
                .findTopByProcesoDiarioIdOrderByNumeroCorteDesc(procesoDiarioId);

        if (ultimoCorte.isEmpty()) {
            return new CortePlantillaResponse(
                    proceso.getId(),
                    (short) 1,
                    LocalTime.of(7, 0),
                    LocalTime.of(8, 0),
                    null,
                    List.of()
            );
        }

        Corte anterior = ultimoCorte.get();

        List<CorteDetalleDTO> detallesClonados = anterior.getDetalles().stream()
                .map(d -> new CorteDetalleDTO(
                        null,
                        d.getMaquina().getId(),
                        d.getMaquina().getNombre(),
                        d.getMaquinaKato().getId(),
                        d.getMaquinaKato().getNombre(),
                        d.getVariedad().getId(),
                        d.getVariedad().getNombre(),
                        BigDecimal.ZERO,       // jabas: varia cada hora, se deja en cero
                        BigDecimal.ZERO,       // peso total: varia cada hora, se deja en cero
                        d.getEmpacadores(),    // personal: casi no cambia en el dia, se mantiene
                        BigDecimal.ZERO,       // kg por empacador: se recalcula, se deja en cero
                        d.getOrden()
                ))
                .toList();

        return new CortePlantillaResponse(
                proceso.getId(),
                (short) (anterior.getNumeroCorte() + 1),
                anterior.getHoraFin(),
                anterior.getHoraFin().plusHours(1),
                anterior.getFechaCosecha(),
                detallesClonados
        );
    }

    // ==========================================================
    // 2. CREAR CORTE (idempotente via clienteUuid)
    // ==========================================================

    @Transactional
    public CorteResponse crear(Long procesoDiarioId, CorteRequest request, String emailUsuario) {
        Optional<Corte> existente = corteRepository.findByClienteUuid(request.clienteUuid());
        if (existente.isPresent()) {
            return toResponse(existente.get());
        }

        ProcesoDiario proceso = procesoDiarioRepository.findById(procesoDiarioId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", procesoDiarioId));

        Short siguienteNumero = corteRepository
                .findTopByProcesoDiarioIdOrderByNumeroCorteDesc(procesoDiarioId)
                .map(c -> (short) (c.getNumeroCorte() + 1))
                .orElse((short) 1);

        if (request.jabasTotalAjustado() != null || request.pesoTotalAjustado() != null) {
            exigirMotivoAjuste(request.motivoAjuste());
        }

        Corte corte = Corte.builder()
                .procesoDiario(proceso)
                .numeroCorte(siguienteNumero)
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .fechaCosecha(request.fechaCosecha())
                .observacion(request.observacion())
                .jabasTotalAjustado(request.jabasTotalAjustado())
                .pesoTotalAjustado(request.pesoTotalAjustado())
                .estado(EstadoCorte.BORRADOR)
                .clienteUuid(request.clienteUuid())
                .build();

        aplicarDetalle(corte, request);
        calcularTotales(corte);

        Corte guardado = corteRepository.save(corte);
        return toResponse(guardado);
    }

    // ==========================================================
    // 3. ACTUALIZAR CORTE
    // ==========================================================

    @Transactional
    public CorteResponse actualizar(Long corteId, CorteRequest request, String emailUsuario) {
        Corte corte = corteRepository.findById(corteId)
                .orElseThrow(() -> ResourceNotFoundException.of("Corte", corteId));

        boolean esConsolidado = corte.getEstado() == EstadoCorte.CONSOLIDADO;
        boolean hayAjusteManual = request.jabasTotalAjustado() != null || request.pesoTotalAjustado() != null;

        if (esConsolidado || hayAjusteManual) {
            exigirMotivoAjuste(request.motivoAjuste());
        }

        BigDecimal pesoCalculadoAnterior = corte.getPesoTotalCalculado();
        BigDecimal jabasCalculadoAnterior = corte.getJabasTotalCalculado();

        corte.setHoraInicio(request.horaInicio());
        corte.setHoraFin(request.horaFin());
        corte.setFechaCosecha(request.fechaCosecha());
        corte.setObservacion(request.observacion());
        corte.setJabasTotalAjustado(request.jabasTotalAjustado());
        corte.setPesoTotalAjustado(request.pesoTotalAjustado());

        corte.getDetalles().clear();
        aplicarDetalle(corte, request);
        calcularTotales(corte);

        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", emailUsuario));

        registrarAuditoria(corte, usuario, request.motivoAjuste(),
                pesoCalculadoAnterior, corte.getPesoTotalCalculado(),
                jabasCalculadoAnterior, corte.getJabasTotalCalculado());

        boolean cambioElCalculo = !safeEquals(pesoCalculadoAnterior, corte.getPesoTotalCalculado())
                || !safeEquals(jabasCalculadoAnterior, corte.getJabasTotalCalculado());

        if (cambioElCalculo) {
            marcarRequiereRevisionAguasAbajo(corte);
        }

        Corte guardado = corteRepository.save(corte);
        return toResponse(guardado);
    }

    // ==========================================================
    // 4. CONSOLIDAR
    // ==========================================================

    @Transactional
    public CorteResponse consolidar(Long corteId, String emailUsuario) {
        Corte corte = corteRepository.findById(corteId)
                .orElseThrow(() -> ResourceNotFoundException.of("Corte", corteId));

        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", emailUsuario));

        corte.setEstado(EstadoCorte.CONSOLIDADO);
        corte.setConsolidadoEn(java.time.OffsetDateTime.now());
        corte.setConsolidadoPor(usuario);

        return toResponse(corteRepository.save(corte));
    }

    // ==========================================================
    // 5. LISTAR
    // ==========================================================

    @Transactional(readOnly = true)
    public List<CorteResponse> listarPorProceso(Long procesoDiarioId) {
        return corteRepository.findByProcesoDiarioIdOrderByNumeroCorteAsc(procesoDiarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ==========================================================
    // Helpers privados
    // ==========================================================

    private void aplicarDetalle(Corte corte, CorteRequest request) {
        request.detalles().forEach(d -> {
            Maquina maquina = maquinaRepository.findById(d.maquinaId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Maquina", d.maquinaId()));

            MaquinaKato maquinaKato = maquinaKatoRepository.findById(d.maquinaKatoId())
                    .orElseThrow(() -> ResourceNotFoundException.of("MaquinaKato", d.maquinaKatoId()));

            Variedad variedad = variedadRepository.findById(d.variedadId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Variedad", d.variedadId()));

            corte.getDetalles().add(
                    CorteDetalle.builder()
                            .corte(corte)
                            .maquina(maquina)
                            .maquinaKato(maquinaKato)
                            .variedad(variedad)
                            .jabas(d.jabas() != null ? d.jabas() : BigDecimal.ZERO)
                            .pesoTotal(d.pesoTotal() != null ? d.pesoTotal() : BigDecimal.ZERO)
                            .empacadores(d.empacadores() != null ? d.empacadores() : 0)
                            .kgPorEmpacador(d.kgPorEmpacador() != null ? d.kgPorEmpacador() : BigDecimal.ZERO)
                            .orden(d.orden() != null ? d.orden() : 0)
                            .clienteUuid(d.clienteUuid())
                            .build()
            );
        });
    }

    /**
     * Los totales del corte SIEMPRE se derivan de la suma del detalle
     * (por maquina+kato+variedad). Nunca se aceptan "a mano" en estas
     * columnas - el unico override manual valido es *TotalAjustado.
     */
    private void calcularTotales(Corte corte) {
        BigDecimal jabas = corte.getDetalles().stream()
                .map(CorteDetalle::getJabas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal peso = corte.getDetalles().stream()
                .map(CorteDetalle::getPesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        corte.setJabasTotalCalculado(jabas);
        corte.setPesoTotalCalculado(peso);
    }

    private void exigirMotivoAjuste(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new BusinessRuleException(
                    "Se requiere indicar un motivo para ajustar manualmente un total "
                            + "o para editar un corte ya consolidado."
            );
        }
    }

    private void registrarAuditoria(Corte corte, Usuario usuario, String motivo,
                                     BigDecimal pesoAnterior, BigDecimal pesoNuevo,
                                     BigDecimal jabasAnterior, BigDecimal jabasNuevo) {
        boolean esAjuste = corte.getJabasTotalAjustado() != null || corte.getPesoTotalAjustado() != null;

        AuditoriaEdicion registro = AuditoriaEdicion.builder()
                .entidad("CORTE")
                .entidadId(corte.getId())
                .campo(esAjuste ? "peso_total_ajustado/jabas_total_ajustado" : "detalle_corte")
                .valorAnterior("peso=%s, jabas=%s".formatted(pesoAnterior, jabasAnterior))
                .valorNuevo("peso=%s, jabas=%s".formatted(pesoNuevo, jabasNuevo))
                .tipoCambio(esAjuste ? AuditoriaEdicion.TipoCambio.AJUSTE_MANUAL : AuditoriaEdicion.TipoCambio.EDICION_INPUT)
                .motivo(motivo)
                .usuario(usuario)
                .build();

        auditoriaEdicionRepository.save(registro);
    }

    private void marcarRequiereRevisionAguasAbajo(Corte corteEditado) {
        List<Corte> siguientes = corteRepository
                .findByProcesoDiarioIdOrderByNumeroCorteAsc(corteEditado.getProcesoDiario().getId())
                .stream()
                .filter(c -> c.getNumeroCorte() > corteEditado.getNumeroCorte())
                .filter(c -> c.getJabasTotalAjustado() != null || c.getPesoTotalAjustado() != null)
                .toList();

        siguientes.forEach(c -> {
            c.setRequiereRevision(true);
            corteRepository.save(c);
        });
    }

    private boolean safeEquals(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    private CorteResponse toResponse(Corte corte) {
        List<CorteDetalleDTO> detalles = corte.getDetalles().stream()
                .map(d -> new CorteDetalleDTO(
                        d.getClienteUuid(),
                        d.getMaquina().getId(), d.getMaquina().getNombre(),
                        d.getMaquinaKato().getId(), d.getMaquinaKato().getNombre(),
                        d.getVariedad().getId(), d.getVariedad().getNombre(),
                        d.getJabas(), d.getPesoTotal(),
                        d.getEmpacadores(), d.getKgPorEmpacador(),
                        d.getOrden()
                ))
                .toList();

        return new CorteResponse(
                corte.getId(),
                corte.getProcesoDiario().getId(),
                corte.getNumeroCorte(),
                corte.getHoraInicio(),
                corte.getHoraFin(),
                corte.getFechaCosecha(),
                corte.getObservacion(),
                corte.getJabasTotalCalculado(),
                corte.getJabasTotalAjustado(),
                corte.getJabasTotalEfectivo(),
                corte.getPesoTotalCalculado(),
                corte.getPesoTotalAjustado(),
                corte.getPesoTotalEfectivo(),
                corte.getEstado().name(),
                corte.getConsolidadoEn(),
                corte.isRequiereRevision(),
                corte.getClienteUuid(),
                detalles
        );
    }
}
