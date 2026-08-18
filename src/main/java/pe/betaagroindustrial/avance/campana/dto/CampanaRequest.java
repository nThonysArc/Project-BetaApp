package pe.betaagroindustrial.avance.campana.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CampanaRequest(
        @NotNull Long productoId,
        @NotBlank String nombre,
        @NotNull Short anio,
        @NotNull LocalDate fechaInicio,
        LocalDate fechaFin
) {
}
