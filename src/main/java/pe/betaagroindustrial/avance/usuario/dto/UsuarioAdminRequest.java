package pe.betaagroindustrial.avance.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.betaagroindustrial.avance.usuario.Permiso;
import pe.betaagroindustrial.avance.usuario.Rol;

import java.util.Set;

/**
 * Alta/edicion de un usuario por el super admin.
 *
 * - email/password: obligatorios para todos los roles salvo SUPERVISOR (un supervisor puede existir
 *   sin iniciar sesion). Al EDITAR, la password solo se cambia si viene informada.
 * - permisos: la checklist marcada. Si viene null al CREAR se usa la plantilla del rol. Se
 *   completan solos los permisos de los que dependen. Se ignora para SUPER_ADMIN (tiene todos).
 * - activo: solo al editar (null = no cambia).
 */
public record UsuarioAdminRequest(
        @NotBlank @Size(max = 150) String nombreCompleto,
        @Email @Size(max = 150) String email,
        @Size(min = 8, max = 100) String password,
        @NotNull Rol rol,
        Boolean activo,
        Set<Permiso> permisos
) {
}
