package pe.betaagroindustrial.avance.proceso.dto;

import java.math.BigDecimal;

/**
 * Lo acumulado en los dias ANTERIORES de la campana (jabas). Lo necesita el reporte de Vid para el stock;
 * el dispositivo lo guarda para poder armar el reporte sin conexion.
 */
public record ArrastreResponse(BigDecimal ingresoAnteriorJabas, BigDecimal acumuladoAnteriorJabas) {
}
