package pe.betaagroindustrial.avance.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.betaagroindustrial.avance.usuario.Permiso;

/**
 * Consulta de permisos del usuario autenticado para reglas que dependen de los DATOS de la
 * peticion (ej.: editar un corte de un proceso cerrado), donde @PreAuthorize no alcanza.
 */
public final class AccesoActual {

    private AccesoActual() {
    }

    public static boolean tiene(Permiso permiso) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permiso.authority()));
    }

    public static void exigir(Permiso permiso, String mensaje) {
        if (!tiene(permiso)) {
            throw new AccessDeniedException(mensaje);
        }
    }
}
