package pe.betaagroindustrial.avance.usuario;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.betaagroindustrial.avance.usuario.dto.CatalogoPermisosResponse;
import pe.betaagroindustrial.avance.usuario.dto.UsuarioAdminRequest;
import pe.betaagroindustrial.avance.usuario.dto.UsuarioAdminResponse;

import java.util.List;

/** Administracion de usuarios y permisos. Exclusivo del SUPER_ADMIN. */
@RestController
@RequestMapping("/api/admin/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UsuarioAdminController {

    private final UsuarioAdminService usuarioAdminService;

    @GetMapping
    public ResponseEntity<List<UsuarioAdminResponse>> listar() {
        return ResponseEntity.ok(usuarioAdminService.listar());
    }

    @GetMapping("/catalogo-permisos")
    public ResponseEntity<CatalogoPermisosResponse> catalogo() {
        return ResponseEntity.ok(usuarioAdminService.catalogo());
    }

    @PostMapping
    public ResponseEntity<UsuarioAdminResponse> crear(@Valid @RequestBody UsuarioAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioAdminService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioAdminResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody UsuarioAdminRequest request, Authentication authentication) {
        return ResponseEntity.ok(usuarioAdminService.actualizar(id, request, authentication.getName()));
    }
}
