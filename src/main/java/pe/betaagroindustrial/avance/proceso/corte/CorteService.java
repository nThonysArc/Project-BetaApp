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
import pe.betaagroindustrial.avance.proceso.EstadoProceso;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Corazon del sistema: clonado del corte anterior para armar el formulario
 * simplificado. Cada fila de detalle es maquina + kato + variedad, para
 * poder saber cuantos kilos proceso especificamente cada kato de cada
 * maquina.
 *
 * El punto de cierre real es el ProcesoDiario completo, no cada corte
 * individual: mientras el proceso este ABIERTO, cualquiera de sus cortes
 * se edita libremente (incluso ajustar la primera hora en base a lo que
 * se descubre en la ultima). Una vez CERRADO, editar exige motivo.
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
                    List.of(),
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
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        d.getEmpacadores(),
                        BigDecimal.ZERO,
                        d.getOrden()
                ))
                .toList();

        return new CortePlantillaResponse(
                proceso.getId(),
                (short) (anterior.getNumeroCorte() + 1),
                anterior.getHoraFin(),
                anterior.getHoraFin().plusHours(1),
                anterior.getFechaCosecha(),
                detallesClonados,
                supervisoresDeCorte(anterior, false)
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

        if (proceso.getEstado() == EstadoProceso.CERRADO) {
            throw new BusinessRuleException(
                    "Este proceso diario ya esta cerrado. No se pueden agregar nuevos cortes.");
        }

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
                .clienteUuid(request.clienteUuid())
                .build();

        aplicarDetalle(corte, request);
        aplicarSupervisores(corte, request, corteRepository
                .findTopByProcesoDiarioIdOrderByNumeroCorteDesc(procesoDiarioId));
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

        // Se consulta antes de modificar el corte para no disparar un flush a medias.
        Optional<Corte> corteAnterior = corteRepository
                .findTopByProcesoDiarioIdAndNumeroCorteLessThanOrderByNumeroCorteDesc(
                        corte.getProcesoDiario().getId(), corte.getNumeroCorte());

        boolean procesoEstaCerrado = corte.getProcesoDiario().getEstado() == EstadoProceso.CERRADO;
        boolean hayAjusteManual = request.jabasTotalAjustado() != null || request.pesoTotalAjustado() != null;

        if (procesoEstaCerrado || hayAjusteManual) {
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
        aplicarSupervisores(corte, request, corteAnterior);
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
    // 4. LISTAR / OBTENER
    // ==========================================================

    @Transactional(readOnly = true)
    public List<CorteResponse> listarPorProceso(Long procesoDiarioId) {
        return corteRepository.findByProcesoDiarioIdOrderByNumeroCorteAsc(procesoDiarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CorteResponse obtenerPorId(Long corteId) {
        Corte corte = corteRepository.findById(corteId)
                .orElseThrow(() -> ResourceNotFoundException.of("Corte", corteId));
        return toResponse(corte);
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
     * Fija el supervisor de cada maquina que aparece en el detalle del corte.
     *
     * Regla: UN solo supervisor por maquina y por corte. Para cada maquina:
     *  1. si el request lo trae explicitamente (aunque sea null = sin supervisor), se usa ese;
     *  2. si no, se conserva el que ya tenia este corte (al editar);
     *  3. si no, se hereda el del corte anterior para esa maquina;
     *  4. si tampoco existe, se usa el supervisor sugerido de la maquina.
     *
     * Las filas existentes se actualizan en el lugar (no se borran y recrean)
     * para no chocar con la restriccion UNIQUE (corte_id, maquina_id), ya que
     * Hibernate ejecuta los INSERT antes que los DELETE al hacer flush.
     */
    private void aplicarSupervisores(Corte corte, CorteRequest request, Optional<Corte> corteAnterior) {
        Map<Long, Maquina> maquinasEnUso = new LinkedHashMap<>();
        corte.getDetalles().forEach(d -> maquinasEnUso.putIfAbsent(d.getMaquina().getId(), d.getMaquina()));

        Map<Long, CorteSupervisorDTO> solicitados = leerSupervisoresSolicitados(request, maquinasEnUso);

        Map<Long, Usuario> heredados = new LinkedHashMap<>();
        corteAnterior.ifPresent(anterior -> anterior.getSupervisores()
                .forEach(cm -> heredados.put(cm.getMaquina().getId(), cm.getSupervisor())));

        // Quitar las maquinas que ya no estan en el detalle
        corte.getSupervisores().removeIf(cm -> !maquinasEnUso.containsKey(cm.getMaquina().getId()));

        maquinasEnUso.forEach((maquinaId, maquina) -> {
            Optional<CorteMaquina> existente = corte.getSupervisores().stream()
                    .filter(cm -> cm.getMaquina().getId().equals(maquinaId))
                    .findFirst();

            CorteSupervisorDTO pedido = solicitados.get(maquinaId);

            Usuario supervisor;
            if (pedido != null) {
                Long solicitado = pedido.supervisorId();
                Usuario actual = existente.map(CorteMaquina::getSupervisor).orElse(null);
                // Si no cambia, se conserva aunque el usuario se haya desactivado despues
                // (asi se puede seguir editando un corte historico).
                supervisor = (actual != null && actual.getId().equals(solicitado))
                        ? actual
                        : resolverSupervisor(solicitado);
            } else if (existente.isPresent()) {
                supervisor = existente.get().getSupervisor();
            } else if (heredados.containsKey(maquinaId)) {
                supervisor = heredados.get(maquinaId);
            } else {
                supervisor = maquina.getSupervisor();
            }

            // La observacion es de ESTA hora: nunca se hereda del corte anterior.
            String observacion = pedido != null
                    ? limpiarTexto(pedido.observacion())
                    : existente.map(CorteMaquina::getObservacion).orElse(null);

            if (existente.isPresent()) {
                existente.get().setSupervisor(supervisor);
                existente.get().setObservacion(observacion);
            } else {
                corte.getSupervisores().add(CorteMaquina.builder()
                        .corte(corte)
                        .maquina(maquina)
                        .supervisor(supervisor)
                        .observacion(observacion)
                        .build());
            }
        });
    }

    private String limpiarTexto(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.strip();
    }

    /** maquinaId -> lo pedido para esa maquina (supervisor y observacion). */
    private Map<Long, CorteSupervisorDTO> leerSupervisoresSolicitados(CorteRequest request, Map<Long, Maquina> maquinasEnUso) {
        Map<Long, CorteSupervisorDTO> solicitados = new LinkedHashMap<>();
        if (request.supervisores() == null) {
            return solicitados;
        }

        for (CorteSupervisorDTO s : request.supervisores()) {
            if (!maquinasEnUso.containsKey(s.maquinaId())) {
                throw new BusinessRuleException(
                        "Se indico un supervisor para una maquina que no esta en el detalle del corte.");
            }
            if (solicitados.containsKey(s.maquinaId())) {
                throw new BusinessRuleException(
                        "Solo se permite un supervisor por maquina en cada corte.");
            }
            solicitados.put(s.maquinaId(), s);
        }
        return solicitados;
    }

    private Usuario resolverSupervisor(Long supervisorId) {
        if (supervisorId == null) {
            return null;
        }
        Usuario usuario = usuarioRepository.findById(supervisorId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario (supervisor)", supervisorId));
        if (!usuario.isActivo()) {
            throw new BusinessRuleException("El usuario " + usuario.getNombreCompleto() + " esta inactivo.");
        }
        return usuario;
    }

    /**
     * Supervisor (y observacion) de cada maquina del corte, en el orden en que
     * aparecen en el detalle. Si una maquina no tiene fila propia, se muestra el
     * supervisor sugerido de la maquina. La plantilla del siguiente corte no
     * arrastra observaciones (conObservacion = false): son de cada hora.
     */
    private List<CorteSupervisorDTO> supervisoresDeCorte(Corte corte, boolean conObservacion) {
        Map<Long, Maquina> maquinasEnUso = new LinkedHashMap<>();
        corte.getDetalles().forEach(d -> maquinasEnUso.putIfAbsent(d.getMaquina().getId(), d.getMaquina()));

        Map<Long, CorteMaquina> filas = new LinkedHashMap<>();
        corte.getSupervisores().forEach(cm -> filas.put(cm.getMaquina().getId(), cm));

        return maquinasEnUso.values().stream()
                .map(m -> {
                    CorteMaquina fila = filas.get(m.getId());
                    Usuario sup = fila != null ? fila.getSupervisor() : m.getSupervisor();
                    return new CorteSupervisorDTO(
                            m.getId(), m.getNombre(),
                            sup != null ? sup.getId() : null,
                            sup != null ? sup.getNombreCompleto() : null,
                            conObservacion && fila != null ? fila.getObservacion() : null
                    );
                })
                .toList();
    }

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
                            + "o para editar un corte de un proceso ya cerrado."
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
                corte.isRequiereRevision(),
                corte.getClienteUuid(),
                detalles,
                supervisoresDeCorte(corte, true)
        );
    }
}
