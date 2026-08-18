package pe.betaagroindustrial.avance.proceso.dto;

import java.time.LocalDate;

public record ProcesoDiarioResponse(
        Long id,
        Long campanaId,
        String campanaNombre,
        LocalDate fecha,
        String estado
) {
}
