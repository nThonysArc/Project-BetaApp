package pe.betaagroindustrial.avance.proceso.materiaprima;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.proceso.ProcesoDiario;
import pe.betaagroindustrial.avance.proceso.ProcesoDiarioRepository;
import pe.betaagroindustrial.avance.proceso.materiaprima.dto.IngresoMateriaPrimaRequest;
import pe.betaagroindustrial.avance.proceso.materiaprima.dto.IngresoMateriaPrimaResponse;
import pe.betaagroindustrial.avance.variedad.Variedad;
import pe.betaagroindustrial.avance.variedad.VariedadRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MateriaPrimaPlanService {

    private final MateriaPrimaPlanRepository materiaPrimaPlanRepository;
    private final ProcesoDiarioRepository procesoDiarioRepository;
    private final VariedadRepository variedadRepository;

    @Transactional(readOnly = true)
    public List<IngresoMateriaPrimaResponse> listar(Long procesoDiarioId) {
        return materiaPrimaPlanRepository.findByProcesoDiarioIdOrderByOrdenAsc(procesoDiarioId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Upsert por variedad: si ya existe un ingreso registrado para esta
     * variedad en este proceso diario, se ACTUALIZA con el nuevo
     * acumulado (no se suma). Si no existe, se crea.
     */
    @Transactional
    public IngresoMateriaPrimaResponse actualizar(Long procesoDiarioId, IngresoMateriaPrimaRequest request) {
        ProcesoDiario proceso = procesoDiarioRepository.findById(procesoDiarioId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", procesoDiarioId));

        Variedad variedad = variedadRepository.findById(request.variedadId())
                .orElseThrow(() -> ResourceNotFoundException.of("Variedad", request.variedadId()));

        MateriaPrimaPlan ingreso = materiaPrimaPlanRepository
                .findByProcesoDiarioIdAndVariedadId(procesoDiarioId, request.variedadId())
                .orElseGet(() -> {
                    short siguienteOrden = (short) materiaPrimaPlanRepository
                            .findByProcesoDiarioIdOrderByOrdenAsc(procesoDiarioId).size();
                    return MateriaPrimaPlan.builder()
                            .procesoDiario(proceso)
                            .variedad(variedad)
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
                ingreso.getVariedad().getId(),
                ingreso.getVariedad().getNombre(),
                ingreso.getJabasPlan(),
                ingreso.getKilosPlan(),
                ingreso.getPesoPromedioPorJaba(),
                ingreso.getUpdatedAt()
        );
    }
}
