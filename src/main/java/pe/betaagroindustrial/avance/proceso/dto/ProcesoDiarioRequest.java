package pe.betaagroindustrial.avance.proceso.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProcesoDiarioRequest(
        @NotNull Long campanaId,
        @NotNull LocalDate fecha
) {
}
