package pe.betaagroindustrial.avance.reporte.vivo;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.proceso.ProcesoDiario;
import pe.betaagroindustrial.avance.proceso.ProcesoDiarioRepository;
import pe.betaagroindustrial.avance.reporte.ReporteDatos;
import pe.betaagroindustrial.avance.reporte.ReporteImagenService;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVidService;
import pe.betaagroindustrial.avance.reporte.vivo.AvanceVersionRepository.Estadistica;

import java.time.Instant;
import java.util.stream.Stream;

/**
 * Avance "en vivo" para la web. Reutiliza los datos del reporte de imagen ({@link ReporteImagenService}) y
 * los pasa por {@link AvanceVivoCalculador}: una sola fuente de verdad para PNG y pantalla.
 *
 * {@link #version} es el pilar del tiempo real por consulta periodica: una huella barata del estado del
 * proceso. La web la pregunta cada ~10 s y solo pide {@link #obtener} cuando la huella cambia.
 */
@Service
@RequiredArgsConstructor
public class AvanceVivoService {

    private final ProcesoDiarioRepository procesoDiarioRepository;
    private final ReporteImagenService reporteImagenService;
    private final AvanceVersionRepository versionRepository;

    @Transactional(readOnly = true)
    public AvanceVivoResponse obtener(Long procesoDiarioId) {
        ProcesoDiario proceso = cargar(procesoDiarioId);
        String version = huella(procesoDiarioId);
        String estado = proceso.getEstado().name();

        if (AvanceVidService.usaPlantillaVid(proceso)) {
            return new AvanceVivoResponse(procesoDiarioId, "VID", estado, false,
                    "La vista en linea de Vid aun no esta disponible. Use la imagen del reporte.",
                    version, Instant.now(), null);
        }

        ReporteDatos datos = reporteImagenService.construirDatos(proceso);
        return new AvanceVivoResponse(procesoDiarioId, "ARANDANO", estado, true, null,
                version, Instant.now(), AvanceVivoCalculador.calcular(datos));
    }

    @Transactional(readOnly = true)
    public AvanceVersionDTO version(Long procesoDiarioId) {
        cargar(procesoDiarioId);
        return new AvanceVersionDTO(procesoDiarioId, huella(procesoDiarioId), Instant.now());
    }

    // ----------------------------------------------------------------------

    private ProcesoDiario cargar(Long id) {
        return procesoDiarioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", id));
    }

    /** "cantidad:ultimaModificacion" de cada tabla que alimenta el reporte, unidas. Cambia con cualquier edicion. */
    private String huella(Long id) {
        return Stream.of(
                        versionRepository.proceso(id),
                        versionRepository.cortes(id),
                        versionRepository.detalles(id),
                        versionRepository.supervisores(id),
                        versionRepository.materiaPrima(id))
                .map(AvanceVivoService::parte)
                .reduce((a, b) -> a + "|" + b)
                .orElse("");
    }

    private static String parte(Estadistica e) {
        long micros = e.ultimaModificacion() == null ? 0
                : e.ultimaModificacion().getEpochSecond() * 1_000_000L + e.ultimaModificacion().getNano() / 1_000;
        return e.cantidad() + ":" + micros;
    }
}
