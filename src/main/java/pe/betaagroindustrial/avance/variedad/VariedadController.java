package pe.betaagroindustrial.avance.variedad;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.betaagroindustrial.avance.variedad.dto.VariedadRequest;
import pe.betaagroindustrial.avance.variedad.dto.VariedadResponse;

import java.util.List;

@RestController
@RequestMapping("/api/variedades")
@RequiredArgsConstructor
public class VariedadController {

    private final VariedadService variedadService;

    @GetMapping
    public ResponseEntity<List<VariedadResponse>> listar(
            @RequestParam Long productoId,
            @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        return ResponseEntity.ok(variedadService.listarPorProducto(productoId, incluirInactivas));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VariedadResponse> crear(@Valid @RequestBody VariedadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(variedadService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VariedadResponse> actualizar(@PathVariable Long id,
                                                          @Valid @RequestBody VariedadRequest request) {
        return ResponseEntity.ok(variedadService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        variedadService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VariedadResponse> reactivar(@PathVariable Long id) {
        return ResponseEntity.ok(variedadService.reactivar(id));
    }
}
