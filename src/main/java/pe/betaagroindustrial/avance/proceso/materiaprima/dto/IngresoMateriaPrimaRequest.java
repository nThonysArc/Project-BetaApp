package pe.betaagroindustrial.avance.proceso.materiaprima.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Actualiza el acumulado de una variedad (del catalogo) con la ultima
 * cifra reportada por Recepcion - valor absoluto acumulado, no un delta.
 */
public record IngresoMateriaPrimaRequest(
        @NotNull Long variedadId,
        @NotNull @PositiveOrZero BigDecimal jabas,
        @NotNull @PositiveOrZero BigDecimal kilos
) {
}
