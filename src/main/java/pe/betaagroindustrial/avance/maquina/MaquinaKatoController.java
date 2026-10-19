package pe.betaagroindustrial.avance.maquina;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.betaagroindustrial.avance.maquina.dto.MaquinaKatoRequest;
import pe.betaagroindustrial.avance.maquina.dto.MaquinaKatoResponse;

import java.util.List;

@RestController
@RequestMapping("/api/maquina-katos")
@RequiredArgsConstructor
public class MaquinaKatoController {

    private final MaquinaKatoService maquinaKatoService;

    @GetMapping
    public ResponseEntity<List<MaquinaKatoResponse>> listar(
            @RequestParam Long maquinaId,
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return ResponseEntity.ok(maquinaKatoService.listarPorMaquina(maquinaId, incluirInactivos));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CATALOGOS_ADMINISTRAR')")
    public ResponseEntity<MaquinaKatoResponse> crear(@Valid @RequestBody MaquinaKatoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maquinaKatoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CATALOGOS_ADMINISTRAR')")
    public ResponseEntity<MaquinaKatoResponse> actualizar(@PathVariable Long id,
                                                             @Valid @RequestBody MaquinaKatoRequest request) {
        return ResponseEntity.ok(maquinaKatoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CATALOGOS_ADMINISTRAR')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        maquinaKatoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasAuthority('PERM_CATALOGOS_ADMINISTRAR')")
    public ResponseEntity<MaquinaKatoResponse> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(maquinaKatoService.reactivar(id));
    }
}
