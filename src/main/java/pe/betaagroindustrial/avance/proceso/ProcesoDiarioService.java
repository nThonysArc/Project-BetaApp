package pe.betaagroindustrial.avance.proceso;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.campana.Campana;
import pe.betaagroindustrial.avance.campana.CampanaRepository;
import pe.betaagroindustrial.avance.common.exception.BusinessRuleException;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.proceso.corte.CorteRepository;
import pe.betaagroindustrial.avance.proceso.dto.ProcesoDiarioRequest;
import pe.betaagroindustrial.avance.proceso.dto.ArrastreResponse;
import pe.betaagroindustrial.avance.proceso.dto.ProcesoDiarioResponse;
import pe.betaagroindustrial.avance.proceso.materiaprima.MateriaPrimaPlanRepository;
import pe.betaagroindustrial.avance.proceso.dto.ProyeccionRequest;
import pe.betaagroindustrial.avance.proceso.dto.StockAnteriorRequest;
import pe.betaagroindustrial.avance.common.exception.BusinessRuleException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcesoDiarioService {

    private final ProcesoDiarioRepository procesoDiarioRepository;
    private final CampanaRepository campanaRepository;
    private final CorteRepository corteRepository;
    private final MateriaPrimaPlanRepository materiaPrimaPlanRepository;

    @Transactional(readOnly = true)
    public ProcesoDiarioResponse obtenerPorId(Long id) {
        return toResponse(buscarOFallar(id));
    }

    /**
     * Util para el flujo tipico del operador: "dame el proceso de hoy para
     * esta campana" sin tener que conocer el id numerico de antemano.
     */
    @Transactional(readOnly = true)
    public ProcesoDiarioResponse obtenerPorCampanaYFecha(Long campanaId, LocalDate fecha) {
        return procesoDiarioRepository.findByCampanaIdAndFecha(campanaId, fecha)
                .map(this::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "ProcesoDiario", "campana=%s, fecha=%s".formatted(campanaId, fecha)));
    }

    /**
     * Historial completo de procesos de una campana (abiertos y cerrados),
     * ordenado del mas reciente al mas antiguo - permite volver a
     * cualquier dia anterior para revisar o corregir, no solo el de hoy.
     */
    @Transactional(readOnly = true)
    public List<ProcesoDiarioResponse> listarPorCampana(Long campanaId) {
        return procesoDiarioRepository.findByCampanaIdOrderByFechaDesc(campanaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProcesoDiarioResponse crear(ProcesoDiarioRequest request) {
        Campana campana = campanaRepository.findById(request.campanaId())
                .orElseThrow(() -> ResourceNotFoundException.of("Campana", request.campanaId()));

        procesoDiarioRepository.findByCampanaIdAndFecha(campana.getId(), request.fecha())
                .ifPresent(p -> {
                    throw new BusinessRuleException(
                            "Ya existe un proceso diario para esta campana en la fecha " + request.fecha());
                });

        ProcesoDiario proceso = ProcesoDiario.builder()
                .campana(campana)
                .fecha(request.fecha())
                .estado(EstadoProceso.ABIERTO)
                .build();

        return toResponse(procesoDiarioRepository.save(proceso));
    }

    @Transactional
    public ProcesoDiarioResponse cerrar(Long id) {
        ProcesoDiario proceso = buscarOFallar(id);
        proceso.setEstado(EstadoProceso.CERRADO);
        return toResponse(procesoDiarioRepository.save(proceso));
    }

    /**
     * Reabre un proceso cerrado - por si se cerro por error, o se necesita
     * agregar un corte nuevo a un dia que ya se habia dado por terminado.
     */
    @Transactional
    public ProcesoDiarioResponse reabrir(Long id) {
        ProcesoDiario proceso = buscarOFallar(id);
        proceso.setEstado(EstadoProceso.ABIERTO);
        return toResponse(procesoDiarioRepository.save(proceso));
    }

    /** Jabas ingresadas y procesadas en los dias previos de la campana (0 si no hay). */
    @Transactional(readOnly = true)
    public ArrastreResponse arrastre(Long id) {
        ProcesoDiario proceso = buscarOFallar(id);
        Long campanaId = proceso.getCampana().getId();
        java.math.BigDecimal ingreso = materiaPrimaPlanRepository.sumarJabasAnteriores(campanaId, proceso.getFecha());
        java.math.BigDecimal procesado = corteRepository.sumarJabasAnteriores(campanaId, proceso.getFecha());
        return new ArrastreResponse(
                ingreso == null ? java.math.BigDecimal.ZERO : ingreso,
                procesado == null ? java.math.BigDecimal.ZERO : procesado);
    }

    /** Guarda (o borra, con null) la proyeccion de jabas del dia. Repetir la misma llamada deja el mismo resultado. */
    @Transactional
    public ProcesoDiarioResponse actualizarProyeccion(Long id, ProyeccionRequest request) {
        ProcesoDiario proceso = buscarOFallar(id);
        proceso.setProyeccionJabas(request.jabas());
        return toResponse(procesoDiarioRepository.save(proceso));
    }

    /**
     * Guarda (o borra, con jabas null) el stock que quedo de un dia previo. Es un "poner este valor": repetir la
     * llamada deja el mismo resultado, asi que reintentar tras un corte de red no duplica nada.
     */
    @Transactional
    public ProcesoDiarioResponse actualizarStockAnterior(Long id, StockAnteriorRequest request) {
        ProcesoDiario proceso = buscarOFallar(id);
        if (request.jabas() == null) {
            proceso.setStockAnteriorJabas(null);
            proceso.setStockAnteriorFecha(null);
        } else {
            LocalDate fecha = request.fecha() != null ? request.fecha() : proceso.getFecha().minusDays(1);
            if (!fecha.isBefore(proceso.getFecha())) {
                throw new BusinessRuleException("La fecha del stock anterior debe ser previa a la del proceso.");
            }
            proceso.setStockAnteriorJabas(request.jabas());
            proceso.setStockAnteriorFecha(fecha);
        }
        return toResponse(procesoDiarioRepository.save(proceso));
    }

    private ProcesoDiario buscarOFallar(Long id) {
        return procesoDiarioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", id));
    }

    private ProcesoDiarioResponse toResponse(ProcesoDiario proceso) {
        int totalCortes = corteRepository.countByProcesoDiarioId(proceso.getId());
        return new ProcesoDiarioResponse(
                proceso.getId(),
                proceso.getCampana().getId(),
                proceso.getCampana().getNombre(),
                proceso.getFecha(),
                proceso.getEstado().name(),
                totalCortes,
                proceso.getProyeccionJabas(),
                proceso.getStockAnteriorJabas(),
                proceso.getStockAnteriorFecha()
        );
    }
}
