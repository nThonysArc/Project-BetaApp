package pe.betaagroindustrial.avance.proceso;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.betaagroindustrial.avance.proceso.dto.ProcesoDiarioRequest;
import pe.betaagroindustrial.avance.proceso.dto.ArrastreResponse;
import pe.betaagroindustrial.avance.proceso.dto.ProcesoDiarioResponse;
import pe.betaagroindustrial.avance.proceso.dto.ProyeccionRequest;
import pe.betaagroindustrial.avance.proceso.dto.StockAnteriorRequest;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/procesos")
@RequiredArgsConstructor
public class ProcesoDiarioController {

    private final ProcesoDiarioService procesoDiarioService;

    @PreAuthorize("hasAuthority('PERM_PROCESOS_VER')")
    @GetMapping
    public ResponseEntity<List<ProcesoDiarioResponse>> listarPorCampana(@RequestParam Long campanaId) {
        return ResponseEntity.ok(procesoDiarioService.listarPorCampana(campanaId));
    }

    @PreAuthorize("hasAuthority('PERM_PROCESOS_VER')")
    @GetMapping("/{id}")
    public ResponseEntity<ProcesoDiarioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(procesoDiarioService.obtenerPorId(id));
    }

    /** Acumulados de dias previos (para armar el reporte de Vid en el dispositivo sin conexion). */
    @PreAuthorize("hasAuthority('PERM_PROCESOS_VER')")
    @GetMapping("/{id}/arrastre")
    public ResponseEntity<ArrastreResponse> arrastre(@PathVariable Long id) {
        return ResponseEntity.ok(procesoDiarioService.arrastre(id));
    }

    @PreAuthorize("hasAuthority('PERM_PROCESOS_VER')")
    @GetMapping("/buscar")
    public ResponseEntity<ProcesoDiarioResponse> buscarPorCampanaYFecha(
            @RequestParam Long campanaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(procesoDiarioService.obtenerPorCampanaYFecha(campanaId, fecha));
    }

    @PreAuthorize("hasAuthority('PERM_PROCESOS_CREAR')")
    @PostMapping
    public ResponseEntity<ProcesoDiarioResponse> crear(@Valid @RequestBody ProcesoDiarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procesoDiarioService.crear(request));
    }

    /** Proyeccion de jabas del dia (Vid). Se edita con el mismo permiso que la materia prima: se digitan juntas. */
    @PreAuthorize("hasAuthority('PERM_MATERIA_PRIMA_EDITAR')")
    @PutMapping("/{id}/proyeccion")
    public ResponseEntity<ProcesoDiarioResponse> actualizarProyeccion(
            @PathVariable Long id, @Valid @RequestBody ProyeccionRequest request) {
        return ResponseEntity.ok(procesoDiarioService.actualizarProyeccion(id, request));
    }

    /** Stock que quedo de un dia previo (Vid). Mismo permiso que la materia prima: se digitan juntos. */
    @PreAuthorize("hasAuthority('PERM_MATERIA_PRIMA_EDITAR')")
    @PutMapping("/{id}/stock-anterior")
    public ResponseEntity<ProcesoDiarioResponse> actualizarStockAnterior(
            @PathVariable Long id, @Valid @RequestBody StockAnteriorRequest request) {
        return ResponseEntity.ok(procesoDiarioService.actualizarStockAnterior(id, request));
    }

    @PreAuthorize("hasAuthority('PERM_PROCESOS_CERRAR')")
    @PostMapping("/{id}/cerrar")
    public ResponseEntity<ProcesoDiarioResponse> cerrar(@PathVariable Long id) {
        return ResponseEntity.ok(procesoDiarioService.cerrar(id));
    }

    @PreAuthorize("hasAuthority('PERM_PROCESOS_REABRIR')")
    @PostMapping("/{id}/reabrir")
    public ResponseEntity<ProcesoDiarioResponse> reabrir(@PathVariable Long id) {
        return ResponseEntity.ok(procesoDiarioService.reabrir(id));
    }
}
