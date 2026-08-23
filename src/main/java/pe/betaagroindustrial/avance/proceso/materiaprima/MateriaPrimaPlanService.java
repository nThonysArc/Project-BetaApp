package pe.betaagroindustrial.avance.proceso.materiaprima;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.proceso.ProcesoDiario;
import pe.betaagroindustrial.avance.proceso.ProcesoDiarioRepository;
import pe.betaagroindustrial.avance.proceso.materiaprima.dto.IngresoMateriaPrimaRequest;
import pe.betaagroindustrial.avance.proceso.materiaprima.dto.IngresoMateriaPrimaResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MateriaPrimaPlanService {

    private final MateriaPrimaPlanRepository materiaPrimaPlanRepository;
    private final ProcesoDiarioRepository procesoDiarioRepository;

    @Transactional(readOnly = true)
    public List<IngresoMateriaPrimaResponse> listar(Long procesoDiarioId) {
        return materiaPrimaPlanRepository.findByProcesoDiarioIdOrderByOrdenAsc(procesoDiarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Upsert: si ya existe un ingreso registrado para esta variedad en este
     * proceso diario, se ACTUALIZA con el nuevo acumulado (no se suma).
     * Si no existe, se crea. Asi cada actualizacion de Recepcion simplemente
     * reemplaza el valor anterior por el mas reciente.
     */
    @Transactional
    public IngresoMateriaPrimaResponse actualizar(Long procesoDiarioId, IngresoMateriaPrimaRequest request) {
        ProcesoDiario proceso = procesoDiarioRepository.findById(procesoDiarioId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", procesoDiarioId));

        MateriaPrimaPlan ingreso = materiaPrimaPlanRepository
                .findByProcesoDiarioIdAndVariedadIgnoreCase(procesoDiarioId, request.variedad())
                .orElseGet(() -> {
                    short siguienteOrden = (short) materiaPrimaPlanRepository
                            .findByProcesoDiarioIdOrderByOrdenAsc(procesoDiarioId).size();
                    return MateriaPrimaPlan.builder()
                            .procesoDiario(proceso)
                            .variedad(request.variedad())
                            .orden(siguienteOrden)
                            .build();
                });

        ingreso.setJabasPlan(request.jabas());
        ingreso.setKilosPlan(request.kilos());

        return toResponse(materiaPrimaPlanRepository.save(ingreso));
    }

    private IngresoMateriaPrimaResponse toResponse(MateriaPrimaPlan ingreso) {
        return new IngresoMateriaPrimaResponse(
                ingreso.getId(),
                ingreso.getVariedad(),
                ingreso.getJabasPlan(),
                ingreso.getKilosPlan(),
                ingreso.getPesoPromedioPorJaba(),
                ingreso.getUpdatedAt()
        );
    }
}
