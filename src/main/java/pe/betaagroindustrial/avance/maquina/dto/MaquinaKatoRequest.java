package pe.betaagroindustrial.avance.maquina.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MaquinaKatoRequest(
        @NotNull Long maquinaId,
        @NotBlank String nombre,
        Short orden
) {
}
