package pe.betaagroindustrial.avance.usuario;

/**
 * Rol base del usuario. SUPER_ADMIN tiene TODOS los permisos y no se puede restringir; los demas
 * roles son una etiqueta + plantilla inicial: lo que cada usuario puede hacer realmente lo define
 * su checklist de permisos (ver {@link Permiso}).
 */
public enum Rol {
    SUPER_ADMIN,
    ADMIN,
    SUPERVISOR,
    OPERADOR
}
