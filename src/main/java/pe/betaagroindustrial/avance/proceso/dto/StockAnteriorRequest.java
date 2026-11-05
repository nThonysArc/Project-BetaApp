package pe.betaagroindustrial.avance.proceso.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Stock que quedo de un dia previo. jabas = null borra el dato (el reporte vuelve a deducirlo del historial);
 * fecha = null usa el dia anterior al del proceso.
 */
public record StockAnteriorRequest(
        @Min(0) @Digits(integer = 10, fraction = 0) BigDecimal jabas,
        LocalDate fecha
) {
}
