package pe.betaagroindustrial.avance.proceso;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.campana.Campana;
import pe.betaagroindustrial.avance.campana.CampanaRepository;
import pe.betaagroindustrial.avance.common.exception.BusinessRuleException;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.proceso.dto.ProcesoDiarioRequest;
import pe.betaagroindustrial.avance.proceso.dto.ProcesoDiarioResponse;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProcesoDiarioService {

    private final ProcesoDiarioRepository procesoDiarioRepository;
    private final CampanaRepository campanaRepository;

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

    private ProcesoDiario buscarOFallar(Long id) {
        return procesoDiarioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", id));
    }

    private ProcesoDiarioResponse toResponse(ProcesoDiario proceso) {
        return new ProcesoDiarioResponse(
                proceso.getId(),
                proceso.getCampana().getId(),
                proceso.getCampana().getNombre(),
                proceso.getFecha(),
                proceso.getEstado().name()
        );
    }
}
