package pe.betaagroindustrial.avance.usuario.dto;

import java.util.List;

public record UsuarioAdminResponse(
        Long id,
        String nombreCompleto,
        String email,
        String rol,
        boolean activo,
        boolean puedeIniciarSesion,
        List<String> permisos
) {
}
