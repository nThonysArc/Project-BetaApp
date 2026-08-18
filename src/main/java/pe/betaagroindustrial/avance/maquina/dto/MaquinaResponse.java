package pe.betaagroindustrial.avance.maquina.dto;

public record MaquinaResponse(
        Long id,
        Long campanaId,
        String nombre,
        Long supervisorId,
        String supervisorNombre,
        Short orden,
        boolean activo
) {
}
