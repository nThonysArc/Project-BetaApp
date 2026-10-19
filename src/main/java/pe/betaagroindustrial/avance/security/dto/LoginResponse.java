package pe.betaagroindustrial.avance.security.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String email,
        String nombreCompleto,
        String rol,
        List<String> permisos
) {
}
