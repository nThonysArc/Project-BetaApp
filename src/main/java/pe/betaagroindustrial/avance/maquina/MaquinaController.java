package pe.betaagroindustrial.avance.maquina;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.betaagroindustrial.avance.maquina.dto.MaquinaRequest;
import pe.betaagroindustrial.avance.maquina.dto.MaquinaResponse;

import java.util.List;

@RestController
@RequestMapping("/api/maquinas")
@RequiredArgsConstructor
public class MaquinaController {

    private final MaquinaService maquinaService;

    @GetMapping
    public ResponseEntity<List<MaquinaResponse>> listarPorCampana(
            @RequestParam Long campanaId,
            @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        return ResponseEntity.ok(maquinaService.listarPorCampana(campanaId, incluirInactivas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaquinaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(maquinaService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CATALOGOS_ADMINISTRAR')")
    public ResponseEntity<MaquinaResponse> crear(@Valid @RequestBody MaquinaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maquinaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CATALOGOS_ADMINISTRAR')")
    public ResponseEntity<MaquinaResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody MaquinaRequest request) {
        return ResponseEntity.ok(maquinaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CATALOGOS_ADMINISTRAR')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        maquinaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasAuthority('PERM_CATALOGOS_ADMINISTRAR')")
    public ResponseEntity<MaquinaResponse> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(maquinaService.reactivar(id));
    }
}
