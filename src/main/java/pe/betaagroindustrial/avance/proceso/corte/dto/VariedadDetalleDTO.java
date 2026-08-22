package pe.betaagroindustrial.avance.proceso.corte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record VariedadDetalleDTO(
        UUID clienteUuid,
        @NotBlank String variedad,
        @PositiveOrZero BigDecimal jabas,
        @PositiveOrZero BigDecimal pesoPorViaje,
        Short orden
) {
}
