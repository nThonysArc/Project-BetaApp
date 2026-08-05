package pe.betaagroindustrial.avance.security.dto;

public record LoginResponse(
        String token,
        String email,
        String nombreCompleto,
        String rol
) {
}
