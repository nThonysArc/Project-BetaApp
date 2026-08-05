package pe.betaagroindustrial.avance.security;

/**
 * Representacion minima del usuario autenticado, usada como "principal"
 * dentro del SecurityContext una vez validado el JWT.
 *
 * Se ira completando cuando se implemente JwtAuthFilter + UserDetailsServiceImpl.
 */
public record AuthenticatedUser(Long id, String email, String rol) {
}
