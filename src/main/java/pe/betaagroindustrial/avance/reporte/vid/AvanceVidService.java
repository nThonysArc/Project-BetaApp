package pe.betaagroindustrial.avance.reporte.vid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.producto.PlantillaReporte;
import pe.betaagroindustrial.avance.proceso.ProcesoDiario;
import pe.betaagroindustrial.avance.proceso.ProcesoDiarioRepository;
import pe.betaagroindustrial.avance.proceso.corte.Corte;
import pe.betaagroindustrial.avance.proceso.corte.CorteDetalle;
import pe.betaagroindustrial.avance.proceso.corte.CorteMaquina;
import pe.betaagroindustrial.avance.proceso.corte.CorteRepository;
import pe.betaagroindustrial.avance.proceso.materiaprima.MateriaPrimaPlan;
import pe.betaagroindustrial.avance.proceso.materiaprima.MateriaPrimaPlanRepository;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.Contexto;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.HoraVid;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.ModuloHora;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.Resultado;
import pe.betaagroindustrial.avance.usuario.Usuario;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Une el modelo JPA con el calculo puro de {@link AvanceVid}: traduce entidades a datos planos,
 * calcula, valida y dibuja. Es la unica clase del reporte VID que conoce el repositorio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvanceVidService {

    private static final double ESCALA = 2.0;
    private static final String RUTA_LOGO = "/reporte/logo-beta.png";

    private final ProcesoDiarioRepository procesoDiarioRepository;
    private final CorteRepository corteRepository;
    private final MateriaPrimaPlanRepository materiaPrimaPlanRepository;

    private record Calculo(ProcesoDiario proceso, List<HoraVid> horas, Resultado resultado, List<Advertencia> advertencias) {
    }

    /** Advertencias de consistencia del dia. Vacio si el producto no usa la plantilla VID. */
    @Transactional(readOnly = true)
    public List<Advertencia> advertencias(Long procesoDiarioId) {
        ProcesoDiario proceso = cargar(procesoDiarioId);
        if (!usaPlantillaVid(proceso)) {
            return List.of();
        }
        return calcular(proceso).advertencias();
    }

    @Transactional(readOnly = true)
    public byte[] generarPng(Long procesoDiarioId) {
        return generarPng(cargar(procesoDiarioId));
    }

    /** Variante para quien ya cargo el proceso (evita volver a consultarlo). */
    @Transactional(readOnly = true)
    public byte[] generarPng(ProcesoDiario proceso) {
        Calculo c = calcular(proceso);
        Short anio = proceso.getCampana().getAnio();
        String producto = proceso.getCampana().getProducto().getNombre().toUpperCase(Locale.ROOT);
        String titulo = "AVANCE POR HORA DE SELECCIÓN " + producto + (anio != null ? " " + anio : "");
        return ReporteVidRenderer.renderizarPng(
                new ReporteVidRenderer.Entrada(titulo, proceso.getFecha(), c.resultado(), c.advertencias(), leerLogo()),
                ESCALA);
    }

    public static boolean usaPlantillaVid(ProcesoDiario proceso) {
        return proceso.getCampana().getProducto().getPlantillaReporte() == PlantillaReporte.VID;
    }

    // ----------------------------------------------------------------------

    private ProcesoDiario cargar(Long id) {
        return procesoDiarioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", id));
    }

    private Calculo calcular(ProcesoDiario proceso) {
        Long procesoId = proceso.getId();
        Long campanaId = proceso.getCampana().getId();

        List<Corte> cortes = corteRepository.findByProcesoDiarioIdOrderByNumeroCorteAsc(procesoId);
        List<HoraVid> horas = cortes.stream().map(this::mapearHora).toList();

        BigDecimal ingresoDia = materiaPrimaPlanRepository.findByProcesoDiarioIdOrderByOrdenAsc(procesoId).stream()
                .map(MateriaPrimaPlan::getJabasPlan)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Contexto ctx = new Contexto(
                proceso.getFecha(),
                ingresoDia,
                materiaPrimaPlanRepository.sumarJabasAnteriores(campanaId, proceso.getFecha()),
                corteRepository.sumarJabasAnteriores(campanaId, proceso.getFecha()));

        Resultado resultado = AvanceVid.calcular(horas, ctx);
        return new Calculo(proceso, horas, resultado, ValidadorAvanceVid.validar(horas, resultado));
    }

    private HoraVid mapearHora(Corte corte) {
        List<CorteDetalle> detalles = new ArrayList<>(corte.getDetalles());
        detalles.sort(Comparator.comparing((CorteDetalle d) -> d.getOrden() == null ? 0 : d.getOrden())
                .thenComparing(d -> d.getId() == null ? 0L : d.getId()));

        Map<Long, CorteMaquina> porMaquina = new LinkedHashMap<>();
        corte.getSupervisores().forEach(cm -> porMaquina.put(cm.getMaquina().getId(), cm));

        List<ModuloHora> modulos = new ArrayList<>();
        for (CorteDetalle d : detalles) {
            CorteMaquina cm = porMaquina.get(d.getMaquina().getId());
            Usuario sup = cm != null ? cm.getSupervisor() : d.getMaquina().getSupervisor();
            modulos.add(new ModuloHora(
                    d.getMaquina().getId(),
                    d.getMaquina().getNombre(),
                    d.getVariedad().getNombre(),
                    d.getEmpacadores() == null ? 0 : d.getEmpacadores(),
                    d.getJabas(),
                    sup != null ? sup.getNombreCompleto() : null,
                    cm != null ? cm.getObservacion() : null,
                    cm != null && cm.isAlmuerzo()));
        }
        return new HoraVid(
                corte.getNumeroCorte(),
                corte.getHoraInicio(),
                corte.getHoraFin(),
                corte.getFechaCosecha(),
                corte.getJabasTotalAjustado(),
                modulos);
    }

    private byte[] leerLogo() {
        try (InputStream in = AvanceVidService.class.getResourceAsStream(RUTA_LOGO)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            log.warn("No se pudo leer el logo {}", RUTA_LOGO, e);
            return null;
        }
    }
}
