package pe.betaagroindustrial.avance.usuario;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.BusinessRuleException;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.usuario.dto.CatalogoPermisosResponse;
import pe.betaagroindustrial.avance.usuario.dto.UsuarioAdminRequest;
import pe.betaagroindustrial.avance.usuario.dto.UsuarioAdminResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gestion completa de usuarios por el super admin: alta, edicion, cambio de clave, activar o
 * desactivar y checklist de permisos. El control de acceso a este servicio vive en el controlador
 * (solo SUPER_ADMIN).
 */
@Service
@RequiredArgsConstructor
public class UsuarioAdminService {

    private static final String DOMINIO_SIN_ACCESO = "@sin-acceso.local";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioAdminResponse> listar() {
        return usuarioRepository.findAllByOrderByNombreCompletoAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CatalogoPermisosResponse catalogo() {
        List<CatalogoPermisosResponse.PermisoItem> items = Arrays.stream(Permiso.values())
                .map(p -> new CatalogoPermisosResponse.PermisoItem(
                        p.name(), p.modulo(), p.accion(), p.descripcion(),
                        p.requiere().stream().map(Enum::name).toList()))
                .toList();

        Map<String, List<String>> plantillas = new LinkedHashMap<>();
        for (Rol rol : Rol.values()) {
            plantillas.put(rol.name(), PlantillasPermisos.de(rol).stream().map(Enum::name).sorted().toList());
        }
        return new CatalogoPermisosResponse(items, plantillas);
    }

    @Transactional
    public UsuarioAdminResponse crear(UsuarioAdminRequest request) {
        boolean traeEmail = request.email() != null && !request.email().isBlank();
        boolean traePassword = request.password() != null && !request.password().isBlank();
        boolean puedeSinAcceso = request.rol() == Rol.SUPERVISOR;

        if (traeEmail != traePassword || (!traeEmail && !puedeSinAcceso)) {
            throw new BusinessRuleException(puedeSinAcceso
                    ? "Indica email y contrasena juntos, o deja ambos vacios si el supervisor no inicia sesion."
                    : "Para este rol el email y la contrasena son obligatorios.");
        }

        String email = traeEmail
                ? request.email().trim().toLowerCase()
                : "usuario-" + UUID.randomUUID() + DOMINIO_SIN_ACCESO;
        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Ya existe un usuario con ese email.");
        }

        Set<Permiso> permisos = request.permisos() != null
                ? request.permisos()
                : PlantillasPermisos.de(request.rol());

        Usuario usuario = Usuario.builder()
                .nombreCompleto(request.nombreCompleto().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(traePassword ? request.password() : UUID.randomUUID().toString()))
                .rol(request.rol())
                .activo(request.activo() == null || request.activo())
                .permisos(permisosAGuardar(request.rol(), permisos))
                .build();

        return toResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioAdminResponse actualizar(Long id, UsuarioAdminRequest request, String emailSolicitante) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));

        boolean esUnoMismo = usuario.getEmail().equalsIgnoreCase(emailSolicitante);
        boolean seraActivo = request.activo() == null ? usuario.isActivo() : request.activo();

        // Un super admin no puede quitarse a si mismo el acceso, y siempre debe quedar al menos uno activo.
        if (usuario.getRol() == Rol.SUPER_ADMIN && usuario.isActivo()
                && (request.rol() != Rol.SUPER_ADMIN || !seraActivo)) {
            if (esUnoMismo) {
                throw new BusinessRuleException("No puedes quitarte el rol de super admin ni desactivarte a ti mismo.");
            }
            if (usuarioRepository.countByRolAndActivoTrue(Rol.SUPER_ADMIN) <= 1) {
                throw new BusinessRuleException("Debe existir al menos un super admin activo.");
            }
        }
        if (esUnoMismo && !seraActivo) {
            throw new BusinessRuleException("No puedes desactivarte a ti mismo.");
        }

        if (request.email() != null && !request.email().isBlank()) {
            String nuevoEmail = request.email().trim().toLowerCase();
            if (!nuevoEmail.equalsIgnoreCase(usuario.getEmail())) {
                if (usuarioRepository.existsByEmail(nuevoEmail)) {
                    throw new BusinessRuleException("Ya existe un usuario con ese email.");
                }
                usuario.setEmail(nuevoEmail);
            }
        }
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        usuario.setNombreCompleto(request.nombreCompleto().trim());
        usuario.setRol(request.rol());
        usuario.setActivo(seraActivo);

        // Reemplazo en el lugar de la coleccion (Hibernate calcula el diff de filas)
        if (request.permisos() != null || request.rol() == Rol.SUPER_ADMIN) {
            usuario.getPermisos().clear();
            usuario.getPermisos().addAll(permisosAGuardar(request.rol(), request.permisos()));
        }

        return toResponse(usuarioRepository.save(usuario));
    }

    /** SUPER_ADMIN no guarda filas (tiene todo); el resto guarda lo marcado mas sus dependencias. */
    private Set<Permiso> permisosAGuardar(Rol rol, Set<Permiso> marcados) {
        if (rol == Rol.SUPER_ADMIN) {
            return new HashSet<>();
        }
        return new HashSet<>(Permiso.expandir(marcados));
    }

    private UsuarioAdminResponse toResponse(Usuario u) {
        return new UsuarioAdminResponse(
                u.getId(), u.getNombreCompleto(), u.getEmail(), u.getRol().name(), u.isActivo(),
                !u.getEmail().endsWith(DOMINIO_SIN_ACCESO),
                u.permisosEfectivos().stream().map(Enum::name).sorted().toList());
    }
}
