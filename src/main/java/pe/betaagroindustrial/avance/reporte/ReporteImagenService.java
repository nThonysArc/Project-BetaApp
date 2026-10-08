package pe.betaagroindustrial.avance.reporte;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.maquina.Maquina;
import pe.betaagroindustrial.avance.proceso.ProcesoDiario;
import pe.betaagroindustrial.avance.proceso.ProcesoDiarioRepository;
import pe.betaagroindustrial.avance.proceso.corte.Corte;
import pe.betaagroindustrial.avance.proceso.corte.CorteDetalle;
import pe.betaagroindustrial.avance.proceso.corte.CorteMaquina;
import pe.betaagroindustrial.avance.proceso.corte.CorteRepository;
import pe.betaagroindustrial.avance.proceso.materiaprima.MateriaPrimaPlan;
import pe.betaagroindustrial.avance.proceso.materiaprima.MateriaPrimaPlanRepository;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.CorteReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.FilaReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.LoteReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.MaquinaCorteReporte;
import pe.betaagroindustrial.avance.usuario.Usuario;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Genera la imagen del reporte de avance del dia: una reproduccion fiel de la
 * plantilla de Excel que hoy se captura a mano.
 *
 * Este servicio solo TRADUCE entidades a {@link ReporteDatos}; el dibujo vive en
 * {@link ReporteExcelRenderer}. La imagen se genera bajo demanda (nunca se
 * persiste), asi que siempre refleja el estado mas reciente y crece con cada corte.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteImagenService {

    /** Factor de escala de salida: 2.0 = el doble de nitidez que la captura de Excel. */
    private static final double ESCALA = 2.0;

    private static final String RUTA_LOGO = "/reporte/logo-beta.png";
    private static final String RUTA_FOTO = "/reporte/producto-%s.png";

    private final ProcesoDiarioRepository procesoDiarioRepository;
    private final CorteRepository corteRepository;
    private final MateriaPrimaPlanRepository materiaPrimaPlanRepository;

    @Transactional(readOnly = true)
    public byte[] generarPng(Long procesoDiarioId) {
        ProcesoDiario proceso = procesoDiarioRepository.findById(procesoDiarioId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", procesoDiarioId));

        List<MateriaPrimaPlan> ingresos = materiaPrimaPlanRepository
                .findByProcesoDiarioIdOrderByOrdenAsc(procesoDiarioId);
        List<Corte> cortes = corteRepository
                .findByProcesoDiarioIdOrderByNumeroCorteAsc(procesoDiarioId);

        String producto = proceso.getCampana().getProducto().getNombre();
        ReporteDatos datos = new ReporteDatos(
                titulo(producto, proceso),
                proceso.getFecha(),
                ingresos.stream().map(i -> mapearIngreso(i, proceso)).toList(),
                cortes.stream().map(c -> mapearCorte(c, proceso)).toList(),
                leerRecurso(RUTA_LOGO),
                leerFoto(producto));

        return ReporteExcelRenderer.renderizarPng(datos, ESCALA);
    }

    // ==========================================================
    // Entidades -> modelo plano
    // ==========================================================

    private String titulo(String producto, ProcesoDiario proceso) {
        String plural = producto.toUpperCase(Locale.ROOT);
        if (!plural.endsWith("S")) {
            plural += "S";
        }
        Short anio = proceso.getCampana().getAnio();
        return "AVANCE DE PRODUCCIÓN CAMPAÑA " + plural + (anio != null ? " " + anio : "");
    }

    private LoteReporte mapearIngreso(MateriaPrimaPlan i, ProcesoDiario proceso) {
        return new LoteReporte(proceso.getFecha(), i.getJabasPlan(), i.getKilosPlan(), i.getVariedad().getNombre());
    }

    private CorteReporte mapearCorte(Corte corte, ProcesoDiario proceso) {
        List<CorteDetalle> detalles = new ArrayList<>(corte.getDetalles());
        detalles.sort(Comparator.comparing((CorteDetalle d) -> d.getOrden() == null ? 0 : d.getOrden())
                .thenComparing(d -> d.getId() == null ? 0L : d.getId()));

        List<FilaReporte> filas = new ArrayList<>();
        for (CorteDetalle d : detalles) {
            filas.add(new FilaReporte(
                    d.getMaquina().getId(),
                    d.getMaquina().getNombre(),
                    d.getMaquinaKato().getNombre(),
                    d.getVariedad().getNombre(),
                    d.getJabas(),
                    d.getPesoTotal(),
                    d.getEmpacadores() == null ? 0 : d.getEmpacadores(),
                    d.getKgPorEmpacador()));
        }

        // Supervisor/observacion por maquina: fila propia del corte; si falta, el sugerido de la maquina.
        Map<Long, MaquinaCorteReporte> porMaquina = new LinkedHashMap<>();
        for (CorteMaquina cm : corte.getSupervisores()) {
            porMaquina.put(cm.getMaquina().getId(), new MaquinaCorteReporte(
                    cm.getMaquina().getId(), nombre(cm.getSupervisor()), cm.getObservacion()));
        }
        for (CorteDetalle d : detalles) {
            Maquina m = d.getMaquina();
            porMaquina.computeIfAbsent(m.getId(),
                    id -> new MaquinaCorteReporte(id, nombre(m.getSupervisor()), null));
        }

        return new CorteReporte(
                corte.getHoraInicio(),
                corte.getHoraFin(),
                corte.getFechaCosecha() != null ? corte.getFechaCosecha() : proceso.getFecha(),
                corte.getObservacion(),
                corte.getJabasTotalEfectivo(),
                corte.getPesoTotalEfectivo(),
                filas,
                new ArrayList<>(porMaquina.values()));
    }

    private String nombre(Usuario u) {
        return u != null ? u.getNombreCompleto() : null;
    }

    // ==========================================================
    // Recursos graficos (opcionales)
    // ==========================================================

    /** Foto del producto: "Arándano" -> /reporte/producto-arandano.png (tambien prueba sin la "s" final). */
    private byte[] leerFoto(String producto) {
        String slug = Normalizer.normalize(producto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        byte[] foto = leerRecurso(RUTA_FOTO.formatted(slug));
        if (foto == null && slug.endsWith("s")) {
            foto = leerRecurso(RUTA_FOTO.formatted(slug.substring(0, slug.length() - 1)));
        }
        return foto;
    }

    /** Devuelve null (y el renderer simplemente omite la imagen) si el recurso no existe. */
    private byte[] leerRecurso(String ruta) {
        try (InputStream in = ReporteImagenService.class.getResourceAsStream(ruta)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            log.warn("No se pudo leer el recurso {}", ruta, e);
            return null;
        }
    }
}
