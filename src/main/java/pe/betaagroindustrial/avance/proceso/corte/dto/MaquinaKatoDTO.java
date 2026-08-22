package pe.betaagroindustrial.avance.proceso.corte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record MaquinaKatoDTO(
        UUID clienteUuid,
        @NotNull Long maquinaId,
        String maquinaNombre,
        @NotBlank String katoNombre,
        @PositiveOrZero Short empacadores,
        @PositiveOrZero BigDecimal kgPorEmpacador,
        Short orden
) {
}
