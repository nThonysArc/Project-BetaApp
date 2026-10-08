package pe.betaagroindustrial.avance.usuario;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.BusinessRuleException;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.usuario.dto.SupervisorRequest;
import pe.betaagroindustrial.avance.usuario.dto.UsuarioResumenResponse;

import java.util.List;
import java.util.UUID;

/**
 * Administracion de supervisores (usuarios con rol SUPERVISOR).
 *
 * La tabla usuario exige email unico y password_hash, pero un supervisor
 * de linea normalmente no inicia sesion: solo se le asigna a maquinas y
 * cortes. Por eso, si no se indican email y contrasena, se guardan un
 * email interno unico y una contrasena aleatoria que nadie conoce - esa
 * cuenta no puede iniciar sesion, y no hace falta cambiar el esquema.
 *
 * Solo se administran usuarios con rol SUPERVISOR desde aqui; ADMIN y
 * OPERADOR no se pueden editar ni desactivar por esta via.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String DOMINIO_SIN_ACCESO = "@sin-acceso.local";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResumenResponse> listarPorRol(Rol rol, boolean incluirInactivos) {
        List<Usuario> usuarios = incluirInactivos
                ? usuarioRepository.findByRolOrderByNombreCompletoAsc(rol)
                : usuarioRepository.findByRolAndActivoTrueOrderByNombreCompletoAsc(rol);
        return usuarios.stream().map(this::toResponse).toList();
    }

    @Transactional
    public UsuarioResumenResponse crearSupervisor(SupervisorRequest request) {
        boolean traeEmail = request.email() != null && !request.email().isBlank();
        boolean traePassword = request.password() != null && !request.password().isBlank();

        if (traeEmail != traePassword) {
            throw new BusinessRuleException(
                    "Para que el supervisor pueda iniciar sesion indica email y contrasena; "
                            + "si no va a iniciar sesion, deja ambos vacios.");
        }

        String email = traeEmail
                ? request.email().trim().toLowerCase()
                : "supervisor-" + UUID.randomUUID() + DOMINIO_SIN_ACCESO;
        String password = traePassword ? request.password() : UUID.randomUUID().toString();

        if (usuarioRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Ya existe un usuario con ese email.");
        }

        Usuario supervisor = Usuario.builder()
                .nombreCompleto(request.nombreCompleto().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .rol(Rol.SUPERVISOR)
                .activo(true)
                .build();

        return toResponse(usuarioRepository.save(supervisor));
    }

    @Transactional
    public UsuarioResumenResponse actualizarSupervisor(Long id, SupervisorRequest request) {
        Usuario supervisor = buscarSupervisor(id);
        supervisor.setNombreCompleto(request.nombreCompleto().trim());
        return toResponse(usuarioRepository.save(supervisor));
    }

    @Transactional
    public void desactivar(Long id) {
        Usuario supervisor = buscarSupervisor(id);
        supervisor.setActivo(false);
        usuarioRepository.save(supervisor);
    }

    @Transactional
    public UsuarioResumenResponse reactivar(Long id) {
        Usuario supervisor = buscarSupervisor(id);
        supervisor.setActivo(true);
        return toResponse(usuarioRepository.save(supervisor));
    }

    private Usuario buscarSupervisor(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Supervisor", id));
        if (usuario.getRol() != Rol.SUPERVISOR) {
            throw new BusinessRuleException("Solo se pueden administrar usuarios con rol SUPERVISOR.");
        }
        return usuario;
    }

    private UsuarioResumenResponse toResponse(Usuario u) {
        return new UsuarioResumenResponse(u.getId(), u.getNombreCompleto(), u.getRol().name(), u.isActivo());
    }
}
