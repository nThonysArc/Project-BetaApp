package pe.betaagroindustrial.avance.reporte;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.BusinessRuleException;
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
import pe.betaagroindustrial.avance.reporte.vid.AvanceVidService;
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

    /** Factor de escala de salida: 3.0 = el triple de nitidez que la captura de Excel (se reduce solo en reportes muy largos). */
    private static final double ESCALA = 3.0;

    private static final String RUTA_LOGO = "/reporte/logo-beta.png";
    private static final String RUTA_FOTO = "/reporte/producto-%s.png";

    private final ProcesoDiarioRepository procesoDiarioRepository;
    private final CorteRepository corteRepository;
    private final MateriaPrimaPlanRepository materiaPrimaPlanRepository;
    private final AvanceVidService avanceVidService;

    @Transactional(readOnly = true)
    public byte[] generarPng(Long procesoDiarioId) {
        ProcesoDiario proceso = procesoDiarioRepository.findById(procesoDiarioId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", procesoDiarioId));

        // Cada producto elige su plantilla: Vid dibuja su propio formato, el resto usa el de arandano.
        if (AvanceVidService.usaPlantillaVid(proceso)) {
            return avanceVidService.generarPng(proceso);
        }

        return ReporteExcelRenderer.renderizarPng(construirDatos(proceso), ESCALA);
    }

    /**
     * Libro de Excel con el mismo diseno del reporte (solo Arandano por ahora). Datos base editables y
     * totales como formulas, para poder seguir trabajando el avance aunque el sistema no este disponible.
     */
    @Transactional(readOnly = true)
    public byte[] generarXlsx(Long procesoDiarioId) {
        ProcesoDiario proceso = procesoDiarioRepository.findById(procesoDiarioId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", procesoDiarioId));
        if (AvanceVidService.usaPlantillaVid(proceso)) {
            throw new BusinessRuleException("La exportacion a Excel aun no esta disponible para este producto (solo Arandano).");
        }
        return ReporteXlsxRenderer.renderizar(construirDatos(proceso));
    }

    /** Nombre del archivo: "AVANCE POR HORA PROCESO ARANDANO FRESCO 2026-11-05.xlsx" (sin tildes ni caracteres raros). */
    @Transactional(readOnly = true)
    public String nombreArchivoXlsx(Long procesoDiarioId) {
        ProcesoDiario proceso = procesoDiarioRepository.findById(procesoDiarioId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", procesoDiarioId));
        String producto = Normalizer.normalize(proceso.getCampana().getProducto().getNombre(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .trim().replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
        return "AVANCE POR HORA PROCESO " + producto + " " + proceso.getFecha() + ".xlsx";
    }

    /**
     * Datos planos del reporte de arandano para un proceso. Los comparten la imagen PNG y la vista web en
     * vivo, asi ambas muestran siempre exactamente los mismos numeros.
     */
    @Transactional(readOnly = true)
    public ReporteDatos construirDatos(ProcesoDiario proceso) {
        Long procesoDiarioId = proceso.getId();
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
        return datos;
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

        List<FilaReporte> filasPorVariedad = new ArrayList<>();
        for (CorteDetalle d : detalles) {
            filasPorVariedad.add(new FilaReporte(
                    d.getMaquina().getId(),
                    d.getMaquina().getNombre(),
                    d.getMaquinaKato() != null ? d.getMaquinaKato().getNombre() : "",
                    d.getVariedad().getNombre(),
                    d.getJabas(),
                    d.getPesoTotal(),
                    d.getEmpacadores() == null ? 0 : d.getEmpacadores(),
                    d.getKgPorEmpacador()));
        }
        List<FilaReporte> filas = fusionarPorKato(filasPorVariedad);

        // Supervisor/observacion por maquina: fila propia del corte; si falta, el sugerido de la maquina.
        Map<Long, MaquinaCorteReporte> porMaquina = new LinkedHashMap<>();
        for (CorteMaquina cm : corte.getSupervisores()) {
            porMaquina.put(cm.getMaquina().getId(), new MaquinaCorteReporte(
                    cm.getMaquina().getId(), nombre(cm.getSupervisor()), cm.getObservacion(), cm.isAlmuerzo()));
        }
        for (CorteDetalle d : detalles) {
            Maquina m = d.getMaquina();
            porMaquina.computeIfAbsent(m.getId(),
                    id -> new MaquinaCorteReporte(id, nombre(m.getSupervisor()), null, false));
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

    /**
     * Una Kato que en la misma hora trabajo varias variedades se muestra como UNA sola fila:
     * variedades unidas ("VENTURA/SEKOYA"), jabas y peso sumados. El detalle por variedad se sigue
     * guardando en el corte; esto solo cambia como se lee el reporte. Empacadores no se suman ni se
     * "elige el mayor": es UN solo equipo por kato (el formulario ya pide una sola cantidad), asi que
     * se toma el de la primera fila de la kato y kg/empacador se recalcula con el peso total.
     * Respeta el orden de aparicion de maquinas y katos.
     */
    static List<FilaReporte> fusionarPorKato(List<FilaReporte> filas) {
        Map<String, List<FilaReporte>> grupos = new LinkedHashMap<>();
        for (FilaReporte f : filas) {
            grupos.computeIfAbsent(f.maquinaId() + "|" + f.kato(), k -> new ArrayList<>()).add(f);
        }
        List<FilaReporte> resultado = new ArrayList<>();
        for (List<FilaReporte> g : grupos.values()) {
            if (g.size() == 1) {
                resultado.add(g.get(0));
                continue;
            }
            FilaReporte primera = g.get(0);
            java.math.BigDecimal jabas = java.math.BigDecimal.ZERO;
            java.math.BigDecimal peso = java.math.BigDecimal.ZERO;
            int empacadores = 0;
            java.util.LinkedHashSet<String> variedades = new java.util.LinkedHashSet<>();
            for (FilaReporte f : g) {
                if (f.jabas() != null) jabas = jabas.add(f.jabas());
                if (f.peso() != null) peso = peso.add(f.peso());
                if (empacadores == 0) {
                    empacadores = f.empacadores();
                }
                variedades.add(f.variedad());
            }
            java.math.BigDecimal kgEmp = empacadores > 0
                    ? peso.divide(java.math.BigDecimal.valueOf(empacadores), 2, java.math.RoundingMode.HALF_UP)
                    : java.math.BigDecimal.ZERO;
            resultado.add(new FilaReporte(primera.maquinaId(), primera.maquina(), primera.kato(),
                    String.join("/", variedades), jabas, peso, empacadores, kgEmp));
        }
        return resultado;
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
