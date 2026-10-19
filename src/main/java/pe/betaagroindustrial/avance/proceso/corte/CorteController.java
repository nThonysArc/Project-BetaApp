package pe.betaagroindustrial.avance.proceso.corte;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.betaagroindustrial.avance.proceso.corte.dto.CortePlantillaResponse;
import pe.betaagroindustrial.avance.proceso.corte.dto.CorteRequest;
import pe.betaagroindustrial.avance.proceso.corte.dto.CorteResponse;

import java.util.List;

/**
 * Flujo tipico desde el frontend:
 *  1. GET  /plantilla-siguiente  -> borrador clonado del corte anterior
 *  2. POST /                     -> guarda el corte real (idempotente via clienteUuid)
 *  3. GET  /{corteId}             -> ver un corte especifico
 *  4. PUT  /{corteId}             -> corrige un corte existente (libre si el
 *                                    proceso esta ABIERTO, exige motivo si CERRADO)
 *
 * El cierre real ocurre a nivel de ProcesoDiario
 * (ver ProcesoDiarioController POST /{id}/cerrar), no por corte individual.
 */
@RestController
@RequestMapping("/api/procesos/{procesoDiarioId}/cortes")
@RequiredArgsConstructor
public class CorteController {

    private final CorteService corteService;

    @PreAuthorize("hasAuthority('PERM_CORTES_CREAR')")
    @GetMapping("/plantilla-siguiente")
    public ResponseEntity<CortePlantillaResponse> obtenerPlantillaSiguiente(@PathVariable Long procesoDiarioId) {
        return ResponseEntity.ok(corteService.obtenerPlantillaSiguiente(procesoDiarioId));
    }

    @PreAuthorize("hasAuthority('PERM_CORTES_VER')")
    @GetMapping
    public ResponseEntity<List<CorteResponse>> listar(@PathVariable Long procesoDiarioId) {
        return ResponseEntity.ok(corteService.listarPorProceso(procesoDiarioId));
    }

    @PreAuthorize("hasAuthority('PERM_CORTES_VER')")
    @GetMapping("/{corteId}")
    public ResponseEntity<CorteResponse> obtener(@PathVariable Long procesoDiarioId, @PathVariable Long corteId) {
        return ResponseEntity.ok(corteService.obtenerPorId(corteId));
    }

    @PreAuthorize("hasAuthority('PERM_CORTES_CREAR')")
    @PostMapping
    public ResponseEntity<CorteResponse> crear(@PathVariable Long procesoDiarioId,
                                                @Valid @RequestBody CorteRequest request,
                                                Authentication authentication) {
        CorteResponse creado = corteService.crear(procesoDiarioId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PreAuthorize("hasAuthority('PERM_CORTES_EDITAR')")
    @PutMapping("/{corteId}")
    public ResponseEntity<CorteResponse> actualizar(@PathVariable Long procesoDiarioId,
                                                      @PathVariable Long corteId,
                                                      @Valid @RequestBody CorteRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(corteService.actualizar(corteId, request, authentication.getName()));
    }
}
