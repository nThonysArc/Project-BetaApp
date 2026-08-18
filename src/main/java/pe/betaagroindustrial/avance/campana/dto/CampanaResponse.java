package pe.betaagroindustrial.avance.campana.dto;

import java.time.LocalDate;

public record CampanaResponse(
        Long id,
        Long productoId,
        String productoNombre,
        String nombre,
        Short anio,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean activa
) {
}
