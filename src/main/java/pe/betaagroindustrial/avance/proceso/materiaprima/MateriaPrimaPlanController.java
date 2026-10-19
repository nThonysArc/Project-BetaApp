package pe.betaagroindustrial.avance.proceso.materiaprima;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.betaagroindustrial.avance.proceso.materiaprima.dto.IngresoMateriaPrimaRequest;
import pe.betaagroindustrial.avance.proceso.materiaprima.dto.IngresoMateriaPrimaResponse;

import java.util.List;

/**
 * Registro del ingreso acumulado de materia prima por variedad, reportado
 * por Recepcion de forma irregular (no cada hora fija). Independiente del
 * registro de cortes: se actualiza cada vez que llega un reporte nuevo.
 */
@RestController
@RequestMapping("/api/procesos/{procesoDiarioId}/materia-prima")
@RequiredArgsConstructor
public class MateriaPrimaPlanController {

    private final MateriaPrimaPlanService materiaPrimaPlanService;

    @PreAuthorize("hasAuthority('PERM_MATERIA_PRIMA_VER')")
    @GetMapping
    public ResponseEntity<List<IngresoMateriaPrimaResponse>> listar(@PathVariable Long procesoDiarioId) {
        return ResponseEntity.ok(materiaPrimaPlanService.listar(procesoDiarioId));
    }

    @PreAuthorize("hasAuthority('PERM_MATERIA_PRIMA_EDITAR')")
    @PutMapping
    public ResponseEntity<IngresoMateriaPrimaResponse> actualizar(
            @PathVariable Long procesoDiarioId,
            @Valid @RequestBody IngresoMateriaPrimaRequest request) {
        return ResponseEntity.ok(materiaPrimaPlanService.actualizar(procesoDiarioId, request));
    }
}
