package pe.betaagroindustrial.avance.proceso.corte;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.BusinessRuleException;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.maquina.Maquina;
import pe.betaagroindustrial.avance.maquina.MaquinaRepository;
import pe.betaagroindustrial.avance.proceso.ProcesoDiario;
import pe.betaagroindustrial.avance.proceso.ProcesoDiarioRepository;
import pe.betaagroindustrial.avance.proceso.auditoria.AuditoriaEdicion;
import pe.betaagroindustrial.avance.proceso.auditoria.AuditoriaEdicionRepository;
import pe.betaagroindustrial.avance.proceso.corte.dto.*;
import pe.betaagroindustrial.avance.usuario.Usuario;
import pe.betaagroindustrial.avance.usuario.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CorteService {

    private final CorteRepository corteRepository;
    private final ProcesoDiarioRepository procesoDiarioRepository;
    private final MaquinaRepository maquinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaEdicionRepository auditoriaEdicionRepository;

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
                    List.of(),
                    List.of()
            );
        }

        Corte anterior = ultimoCorte.get();

        List<VariedadDetalleDTO> variedadesClonadas = anterior.getVariedades().stream()
                .map(v -> new VariedadDetalleDTO(
                        null,
                        v.getVariedad(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        v.getOrden()
                ))
                .toList();

        List<MaquinaKatoDTO> maquinasClonadas = anterior.getMaquinasKato().stream()
                .map(mk -> new MaquinaKatoDTO(
                        null,
                        mk.getMaquina().getId(),
                        mk.getMaquina().getNombre(),
                        mk.getKatoNombre(),
                        mk.getEmpacadores(),
                        BigDecimal.ZERO,
                        mk.getOrden()
                ))
                .toList();

        return new CortePlantillaResponse(
                proceso.getId(),
                (short) (anterior.getNumeroCorte() + 1),
                anterior.getHoraFin(),
                anterior.getHoraFin().plusHours(1),
                anterior.getFechaCosecha(),
                variedadesClonadas,
                maquinasClonadas
        );
    }

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

        corte.getVariedades().clear();
        corte.getMaquinasKato().clear();
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

    @Transactional(readOnly = true)
    public List<CorteResponse> listarPorProceso(Long procesoDiarioId) {
        return corteRepository.findByProcesoDiarioIdOrderByNumeroCorteAsc(procesoDiarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void aplicarDetalle(Corte corte, CorteRequest request) {
        request.variedades().forEach(v -> corte.getVariedades().add(
                CorteVariedadDetalle.builder()
                        .corte(corte)
                        .variedad(v.variedad())
                        .jabas(v.jabas() != null ? v.jabas() : BigDecimal.ZERO)
                        .pesoPorViaje(v.pesoPorViaje() != null ? v.pesoPorViaje() : BigDecimal.ZERO)
                        .orden(v.orden() != null ? v.orden() : 0)
                        .clienteUuid(v.clienteUuid())
                        .build()
        ));

        request.maquinasKato().forEach(mk -> {
            Maquina maquina = maquinaRepository.findById(mk.maquinaId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Maquina", mk.maquinaId()));

            corte.getMaquinasKato().add(
                    CorteMaquinaKato.builder()
                            .corte(corte)
                            .maquina(maquina)
                            .katoNombre(mk.katoNombre())
                            .empacadores(mk.empacadores() != null ? mk.empacadores() : 0)
                            .kgPorEmpacador(mk.kgPorEmpacador() != null ? mk.kgPorEmpacador() : BigDecimal.ZERO)
                            .orden(mk.orden() != null ? mk.orden() : 0)
                            .clienteUuid(mk.clienteUuid())
                            .build()
            );
        });
    }

    private void calcularTotales(Corte corte) {
        BigDecimal jabas = corte.getVariedades().stream()
                .map(CorteVariedadDetalle::getJabas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal peso = corte.getVariedades().stream()
                .map(v -> v.getPesoPorViaje())
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
        List<VariedadDetalleDTO> variedades = corte.getVariedades().stream()
                .map(v -> new VariedadDetalleDTO(
                        v.getClienteUuid(), v.getVariedad(), v.getJabas(), v.getPesoPorViaje(), v.getOrden()))
                .toList();

        List<MaquinaKatoDTO> maquinasKato = corte.getMaquinasKato().stream()
                .map(mk -> new MaquinaKatoDTO(
                        mk.getClienteUuid(), mk.getMaquina().getId(), mk.getMaquina().getNombre(),
                        mk.getKatoNombre(), mk.getEmpacadores(), mk.getKgPorEmpacador(), mk.getOrden()))
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
                variedades,
                maquinasKato
        );
    }
}
