package pe.betaagroindustrial.avance.variedad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VariedadRequest(
        @NotNull Long productoId,
        @NotBlank String nombre,
        Short orden
) {
}
