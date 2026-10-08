package pe.betaagroindustrial.avance.proceso.corte.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Supervisor y observacion de una maquina durante un corte. En el request
 * solo importan maquinaId, supervisorId (null = sin supervisor) y
 * observacion; los nombres se llenan unicamente en las respuestas.
 */
public record CorteSupervisorDTO(
        @NotNull Long maquinaId,
        String maquinaNombre,
        Long supervisorId,
        String supervisorNombre,
        String observacion
) {
}
