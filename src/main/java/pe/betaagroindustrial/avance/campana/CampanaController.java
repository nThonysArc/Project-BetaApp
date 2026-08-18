package pe.betaagroindustrial.avance.campana;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.betaagroindustrial.avance.campana.dto.CampanaRequest;
import pe.betaagroindustrial.avance.campana.dto.CampanaResponse;

import java.util.List;

@RestController
@RequestMapping("/api/campanas")
@RequiredArgsConstructor
public class CampanaController {

    private final CampanaService campanaService;

    @GetMapping
    public ResponseEntity<List<CampanaResponse>> listar() {
        return ResponseEntity.ok(campanaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampanaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(campanaService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampanaResponse> crear(@Valid @RequestBody CampanaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campanaService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampanaResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody CampanaRequest request) {
        return ResponseEntity.ok(campanaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        campanaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
