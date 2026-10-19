package pe.betaagroindustrial.avance.usuario;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.betaagroindustrial.avance.usuario.dto.SupervisorRequest;
import pe.betaagroindustrial.avance.usuario.dto.UsuarioResumenResponse;

import java.util.List;

/**
 * - GET: lista de usuarios por rol (cualquier usuario autenticado; solo
 *   expone id, nombre, rol y estado). Alimenta los selectores de supervisor.
 * - POST/PUT/DELETE/reactivar: administracion de supervisores, solo ADMIN.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResumenResponse>> listarPorRol(
            @RequestParam(defaultValue = "SUPERVISOR") Rol rol,
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return ResponseEntity.ok(usuarioService.listarPorRol(rol, incluirInactivos));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_SUPERVISORES_ADMINISTRAR')")
    public ResponseEntity<UsuarioResumenResponse> crearSupervisor(@Valid @RequestBody SupervisorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearSupervisor(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_SUPERVISORES_ADMINISTRAR')")
    public ResponseEntity<UsuarioResumenResponse> actualizarSupervisor(
            @PathVariable Long id, @Valid @RequestBody SupervisorRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarSupervisor(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_SUPERVISORES_ADMINISTRAR')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        usuarioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasAuthority('PERM_SUPERVISORES_ADMINISTRAR')")
    public ResponseEntity<UsuarioResumenResponse> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.reactivar(id));
    }
}
