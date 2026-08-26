package pe.betaagroindustrial.avance.proceso;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.betaagroindustrial.avance.proceso.dto.ProcesoDiarioRequest;
import pe.betaagroindustrial.avance.proceso.dto.ProcesoDiarioResponse;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/procesos")
@RequiredArgsConstructor
public class ProcesoDiarioController {

    private final ProcesoDiarioService procesoDiarioService;

    @GetMapping
    public ResponseEntity<List<ProcesoDiarioResponse>> listarPorCampana(@RequestParam Long campanaId) {
        return ResponseEntity.ok(procesoDiarioService.listarPorCampana(campanaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcesoDiarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(procesoDiarioService.obtenerPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ProcesoDiarioResponse> buscarPorCampanaYFecha(
            @RequestParam Long campanaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(procesoDiarioService.obtenerPorCampanaYFecha(campanaId, fecha));
    }

    @PostMapping
    public ResponseEntity<ProcesoDiarioResponse> crear(@Valid @RequestBody ProcesoDiarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procesoDiarioService.crear(request));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<ProcesoDiarioResponse> cerrar(@PathVariable Long id) {
        return ResponseEntity.ok(procesoDiarioService.cerrar(id));
    }

    @PostMapping("/{id}/reabrir")
    public ResponseEntity<ProcesoDiarioResponse> reabrir(@PathVariable Long id) {
        return ResponseEntity.ok(procesoDiarioService.reabrir(id));
    }
}
