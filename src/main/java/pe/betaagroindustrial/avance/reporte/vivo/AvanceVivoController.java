package pe.betaagroindustrial.avance.reporte.vivo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Avance del dia para la web (solo lectura). Mismo permiso que la imagen del reporte: quien puede ver
 * el reporte puede verlo en pantalla, sin necesitar ningun permiso de edicion.
 */
@RestController
@RequestMapping("/api/procesos/{procesoDiarioId}/avance")
@RequiredArgsConstructor
public class AvanceVivoController {

    private final AvanceVivoService avanceVivoService;

    /** Avance completo (tabla por hora, pie y resumen por maquina). */
    @PreAuthorize("hasAuthority('PERM_REPORTE_VER')")
    @GetMapping
    public ResponseEntity<AvanceVivoResponse> obtener(@PathVariable Long procesoDiarioId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(avanceVivoService.obtener(procesoDiarioId));
    }

    /** Chequeo barato de "¿cambio algo?" para la consulta periodica de la web. */
    @PreAuthorize("hasAuthority('PERM_REPORTE_VER')")
    @GetMapping("/version")
    public ResponseEntity<AvanceVersionDTO> version(@PathVariable Long procesoDiarioId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(avanceVivoService.version(procesoDiarioId));
    }
}
