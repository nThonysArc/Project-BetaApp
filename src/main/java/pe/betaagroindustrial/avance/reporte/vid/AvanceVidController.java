package pe.betaagroindustrial.avance.reporte.vid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Advertencias de consistencia del avance por hora. Se consultan despues de guardar un corte
 * para avisar al operario sin bloquearlo. Lista vacia si el producto no usa la plantilla VID.
 */
@RestController
@RequestMapping("/api/procesos/{procesoDiarioId}/advertencias")
@RequiredArgsConstructor
public class AvanceVidController {

    private final AvanceVidService avanceVidService;

    @PreAuthorize("hasAuthority('PERM_CORTES_VER')")
    @GetMapping
    public ResponseEntity<List<Advertencia>> listar(@PathVariable Long procesoDiarioId) {
        return ResponseEntity.ok(avanceVidService.advertencias(procesoDiarioId));
    }
}
