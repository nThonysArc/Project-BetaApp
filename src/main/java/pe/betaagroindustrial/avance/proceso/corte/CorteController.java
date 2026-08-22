package pe.betaagroindustrial.avance.proceso.corte;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.betaagroindustrial.avance.proceso.corte.dto.CortePlantillaResponse;
import pe.betaagroindustrial.avance.proceso.corte.dto.CorteRequest;
import pe.betaagroindustrial.avance.proceso.corte.dto.CorteResponse;

import java.util.List;

@RestController
@RequestMapping("/api/procesos/{procesoDiarioId}/cortes")
@RequiredArgsConstructor
public class CorteController {

    private final CorteService corteService;

    @GetMapping("/plantilla-siguiente")
    public ResponseEntity<CortePlantillaResponse> obtenerPlantillaSiguiente(@PathVariable Long procesoDiarioId) {
        return ResponseEntity.ok(corteService.obtenerPlantillaSiguiente(procesoDiarioId));
    }

    @GetMapping
    public ResponseEntity<List<CorteResponse>> listar(@PathVariable Long procesoDiarioId) {
        return ResponseEntity.ok(corteService.listarPorProceso(procesoDiarioId));
    }

    @PostMapping
    public ResponseEntity<CorteResponse> crear(@PathVariable Long procesoDiarioId,
                                                @Valid @RequestBody CorteRequest request,
                                                Authentication authentication) {
        CorteResponse creado = corteService.crear(procesoDiarioId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{corteId}")
    public ResponseEntity<CorteResponse> actualizar(@PathVariable Long procesoDiarioId,
                                                      @PathVariable Long corteId,
                                                      @Valid @RequestBody CorteRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(corteService.actualizar(corteId, request, authentication.getName()));
    }

    @PostMapping("/{corteId}/consolidar")
    public ResponseEntity<CorteResponse> consolidar(@PathVariable Long procesoDiarioId,
                                                       @PathVariable Long corteId,
                                                       Authentication authentication) {
        return ResponseEntity.ok(corteService.consolidar(corteId, authentication.getName()));
    }
}
