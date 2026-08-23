package pe.betaagroindustrial.avance.proceso.materiaprima.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Actualiza el acumulado de una variedad con la ultima cifra reportada
 * por Recepcion (no un delta - el valor absoluto acumulado hasta el momento).
 */
public record IngresoMateriaPrimaRequest(
        @NotBlank String variedad,
        @NotNull @PositiveOrZero BigDecimal jabas,
        @NotNull @PositiveOrZero BigDecimal kilos
) {
}
