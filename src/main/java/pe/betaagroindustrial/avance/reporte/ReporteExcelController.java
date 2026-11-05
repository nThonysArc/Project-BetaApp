package pe.betaagroindustrial.avance.reporte;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Descarga del avance en Excel (.xlsx) con el mismo diseno que la imagen y los totales como formulas. */
@RestController
@RequestMapping("/api/procesos/{procesoDiarioId}/reporte-excel")
@RequiredArgsConstructor
public class ReporteExcelController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteImagenService reporteImagenService;

    @PreAuthorize("hasAuthority('PERM_REPORTE_VER')")
    @GetMapping
    public ResponseEntity<byte[]> descargar(@PathVariable Long procesoDiarioId) {
        byte[] xlsx = reporteImagenService.generarXlsx(procesoDiarioId);

        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(reporteImagenService.nombreArchivoXlsx(procesoDiarioId)).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(xlsx);
    }
}
