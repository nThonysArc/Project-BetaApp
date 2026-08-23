package pe.betaagroindustrial.avance.reporte;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/procesos/{procesoDiarioId}/reporte-imagen")
@RequiredArgsConstructor
public class ReporteImagenController {

    private final ReporteImagenService reporteImagenService;

    @GetMapping
    public ResponseEntity<byte[]> obtenerImagen(@PathVariable Long procesoDiarioId) {
        byte[] png = reporteImagenService.generarPng(procesoDiarioId);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(png);
    }
}
