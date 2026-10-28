package pe.betaagroindustrial.avance.reporte.vid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Calculo del "Avance por hora de seleccion" de Vid.
 *
 * Es Java puro (sin Spring ni JPA): recibe datos planos y devuelve el reporte ya
 * calculado. Asi la aritmetica del reporte (acumulados, rendimiento, resumenes) se
 * prueba sin base de datos y vive en UN solo lugar: ni el renderer ni el front
 * vuelven a sumar nada por su cuenta.
 */
public final class AvanceVid {

    private AvanceVid() {
    }

    // ----------------------------------------------------------------------
    // Entrada
    // ----------------------------------------------------------------------

    /** Lo registrado para un modulo (maquina) en una hora. */
    public record ModuloHora(
            Long maquinaId,
            String modulo,
            String variedad,
            int personas,
            BigDecimal jabas,
            String supervisor,
            String observacion,
            boolean almuerzo
    ) {
        public BigDecimal jabasNz() {
            return jabas == null ? BigDecimal.ZERO : jabas;
        }
    }

    /** Una hora (corte). totalAjustado es el ajuste manual del total (null si no hay). */
    public record HoraVid(
            int numero,
            LocalTime inicio,
            LocalTime fin,
            LocalDate fechaCosecha,
            BigDecimal totalAjustado,
            List<ModuloHora> modulos
    ) {
        public BigDecimal totalCalculado() {
            return modulos.stream().map(ModuloHora::jabasNz).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        /** Total efectivo: el ajuste manual si existe, si no la suma de los modulos. */
        public BigDecimal total() {
            return totalAjustado != null ? totalAjustado : totalCalculado();
        }

        public long minutos() {
            return Duration.between(inicio, fin).toMinutes();
        }

        public boolean todoAlmuerzo() {
            return !modulos.isEmpty() && modulos.stream().allMatch(ModuloHora::almuerzo);
        }
    }

    /**
     * Datos del dia que no salen de los cortes.
     *
     * @param ingresoDia         jabas que ingresaron este dia (la "proyeccion" del encabezado)
     * @param ingresoAnterior    jabas ingresadas en los dias previos de la campana
     * @param acumuladoAnterior  jabas procesadas en los dias previos de la campana
     */
    public record Contexto(
            LocalDate fechaProceso,
            BigDecimal ingresoDia,
            BigDecimal ingresoAnterior,
            BigDecimal acumuladoAnterior
    ) {
        BigDecimal ingresoDiaNz() {
            return ingresoDia == null ? BigDecimal.ZERO : ingresoDia;
        }

        BigDecimal ingresoAnteriorNz() {
            return ingresoAnterior == null ? BigDecimal.ZERO : ingresoAnterior;
        }

        BigDecimal acumuladoAnteriorNz() {
            return acumuladoAnterior == null ? BigDecimal.ZERO : acumuladoAnterior;
        }
    }

    // ----------------------------------------------------------------------
    // Salida
    // ----------------------------------------------------------------------

    /** rendimiento = jabas / personas redondeado; null si no hay personas. */
    public record FilaModulo(
            String modulo,
            int personas,
            BigDecimal jabas,
            BigDecimal rendimiento,
            String observacion,
            boolean almuerzo
    ) {
    }

    public record BloqueHora(
            int numero,
            LocalTime inicio,
            LocalTime fin,
            BigDecimal totalHora,
            BigDecimal acumulado,
            List<FilaModulo> filas
    ) {
    }

    public record ProcesadoDia(LocalDate fecha, BigDecimal jabas) {
    }

    public record ResumenSupervisor(String supervisor, String modulo, String variedades, BigDecimal jabas) {
    }

    public record Resultado(
            List<BloqueHora> bloques,
            BigDecimal proyeccion,
            BigDecimal procesadoDia,
            BigDecimal acumuladoAnterior,
            BigDecimal totalAcumulado,
            BigDecimal stock,
            List<ProcesadoDia> porDia,
            List<ResumenSupervisor> supervisores,
            BigDecimal totalSupervisores
    ) {
        public BigDecimal totalPorDia() {
            return porDia.stream().map(ProcesadoDia::jabas).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    public static final String SIN_SUPERVISOR = "SIN SUPERVISOR";

    // ----------------------------------------------------------------------
    // Calculo
    // ----------------------------------------------------------------------

    public static Resultado calcular(List<HoraVid> horas, Contexto ctx) {
        List<BloqueHora> bloques = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;

        for (HoraVid h : horas) {
            acumulado = acumulado.add(h.total());
            List<FilaModulo> filas = h.modulos().stream()
                    .map(m -> new FilaModulo(m.modulo(), m.personas(), m.jabasNz(),
                            rendimiento(m.jabasNz(), m.personas()), m.observacion(), m.almuerzo()))
                    .toList();
            bloques.add(new BloqueHora(h.numero(), h.inicio(), h.fin(), h.total(), acumulado, filas));
        }
        BigDecimal procesado = acumulado;

        // Procesado por dia de cosecha (27/12 y 28/12 ...), con la suma de los modulos.
        Map<LocalDate, BigDecimal> porDia = new TreeMap<>();
        for (HoraVid h : horas) {
            LocalDate dia = h.fechaCosecha() != null ? h.fechaCosecha() : ctx.fechaProceso();
            porDia.merge(dia, h.totalCalculado(), BigDecimal::add);
        }

        // Resumen por supervisor + modulo: incluye TODOS los modulos que aparecen.
        Map<String, BigDecimal> jabasSup = new LinkedHashMap<>();
        Map<String, Set<String>> variedadesSup = new LinkedHashMap<>();
        Map<String, String[]> nombres = new LinkedHashMap<>();
        for (HoraVid h : horas) {
            for (ModuloHora m : h.modulos()) {
                String sup = m.supervisor() == null || m.supervisor().isBlank() ? SIN_SUPERVISOR : m.supervisor().strip();
                String clave = sup + "\u0000" + m.modulo();
                nombres.putIfAbsent(clave, new String[]{sup, m.modulo()});
                jabasSup.merge(clave, m.jabasNz(), BigDecimal::add);
                Set<String> vars = variedadesSup.computeIfAbsent(clave, k -> new LinkedHashSet<>());
                if (m.variedad() != null && !m.variedad().isBlank() && m.jabasNz().signum() > 0) {
                    vars.add(m.variedad().strip());
                }
            }
        }
        List<ResumenSupervisor> supervisores = new ArrayList<>();
        BigDecimal totalSup = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : jabasSup.entrySet()) {
            String[] n = nombres.get(e.getKey());
            supervisores.add(new ResumenSupervisor(n[0], n[1], String.join("/", variedadesSup.get(e.getKey())), e.getValue()));
            totalSup = totalSup.add(e.getValue());
        }

        BigDecimal totalAcumulado = ctx.acumuladoAnteriorNz().add(procesado);
        BigDecimal stock = ctx.ingresoAnteriorNz().add(ctx.ingresoDiaNz()).subtract(totalAcumulado);

        return new Resultado(
                bloques,
                ctx.ingresoDiaNz(),
                procesado,
                ctx.acumuladoAnteriorNz(),
                totalAcumulado,
                stock,
                porDia.entrySet().stream().map(e -> new ProcesadoDia(e.getKey(), e.getValue())).toList(),
                supervisores,
                totalSup);
    }

    /** Rend/persona SIEMPRE se calcula (jabas / personas); nunca se digita. */
    static BigDecimal rendimiento(BigDecimal jabas, int personas) {
        if (personas <= 0) {
            return null;
        }
        return jabas.divide(BigDecimal.valueOf(personas), 0, RoundingMode.HALF_UP);
    }
}
