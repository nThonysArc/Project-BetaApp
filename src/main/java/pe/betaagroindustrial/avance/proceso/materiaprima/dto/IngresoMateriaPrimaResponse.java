package pe.betaagroindustrial.avance.proceso.materiaprima.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record IngresoMateriaPrimaResponse(
        Long id,
        String variedad,
        BigDecimal jabas,
        BigDecimal kilos,
        BigDecimal pesoPromedioPorJaba,
        Instant actualizadoEn
) {
}
