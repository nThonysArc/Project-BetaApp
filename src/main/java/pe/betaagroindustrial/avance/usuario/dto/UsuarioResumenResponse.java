package pe.betaagroindustrial.avance.usuario.dto;

/**
 * Vista minima de un usuario para selectores y para la pantalla de
 * administracion de supervisores. A proposito NO incluye email ni hash
 * de contrasena.
 */
public record UsuarioResumenResponse(
        Long id,
        String nombreCompleto,
        String rol,
        boolean activo
) {
}
