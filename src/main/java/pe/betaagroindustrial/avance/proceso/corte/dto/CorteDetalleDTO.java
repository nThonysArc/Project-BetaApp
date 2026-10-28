package pe.betaagroindustrial.avance.proceso.corte.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Una fila de detalle del corte: maquina + kato + variedad procesada,
 * jabas, peso total (jabas * peso promedio de la variedad) y como se
 * reparte entre los empacadores de esa fila.
 */
public record CorteDetalleDTO(
        UUID clienteUuid,
        @NotNull Long maquinaId,
        String maquinaNombre,
        Long maquinaKatoId,
        String katoNombre,
        @NotNull Long variedadId,
        String variedadNombre,
        @PositiveOrZero BigDecimal jabas,
        @PositiveOrZero BigDecimal pesoTotal,
        @PositiveOrZero Short empacadores,
        @PositiveOrZero BigDecimal kgPorEmpacador,
        Short orden
) {
}
