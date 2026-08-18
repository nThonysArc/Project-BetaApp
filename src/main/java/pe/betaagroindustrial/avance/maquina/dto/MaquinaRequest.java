package pe.betaagroindustrial.avance.maquina.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MaquinaRequest(
        @NotNull Long campanaId,
        @NotBlank String nombre,
        Long supervisorId,
        Short orden
) {
}
