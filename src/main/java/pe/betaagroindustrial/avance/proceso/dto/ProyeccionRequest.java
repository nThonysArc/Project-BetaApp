package pe.betaagroindustrial.avance.proceso.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

/** jabas = null borra la proyeccion (el reporte vuelve a usar el total ingresado). */
public record ProyeccionRequest(
        @Min(0) @Digits(integer = 10, fraction = 0) BigDecimal jabas
) {
}
