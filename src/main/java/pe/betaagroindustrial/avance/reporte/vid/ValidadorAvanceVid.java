package pe.betaagroindustrial.avance.reporte.vid;

import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.HoraVid;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.ModuloHora;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.Resultado;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reglas de consistencia del avance por hora de Vid. Devuelve advertencias; nunca lanza.
 * Cada regla nace de un error real detectado en el Excel que se llenaba a mano.
 */
public final class ValidadorAvanceVid {

    public static final String DURACION_ATIPICA = "DURACION_ATIPICA";
    public static final String HUECO_HORARIO = "HUECO_HORARIO";
    public static final String MODULO_SIN_PERSONAS = "MODULO_SIN_PERSONAS";
    public static final String PERSONAS_SIN_JABAS = "PERSONAS_SIN_JABAS";
    public static final String VALOR_REPETIDO = "VALOR_REPETIDO";
    public static final String TOTAL_AJUSTADO = "TOTAL_AJUSTADO";
    public static final String DIA_DESCUADRADO = "DIA_DESCUADRADO";
    public static final String SUPERVISOR_DESCUADRADO = "SUPERVISOR_DESCUADRADO";
    public static final String STOCK_NEGATIVO = "STOCK_NEGATIVO";

    static final int MIN_NORMAL = 55;
    static final int MAX_NORMAL = 65;
    /** Cuanto puede diferir la duracion de dos horas para considerarlas "el mismo tiempo". */
    static final int TOLERANCIA_MINUTOS = 5;

    /** "10 mnts de proceso", "15 min", "20 minutos": la observacion declara un tiempo parcial. */
    private static final Pattern MINUTOS = Pattern.compile("\\d+\\s*(mnts?|mins?|minutos?)\\b", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("H:mm");

    private ValidadorAvanceVid() {
    }

    public static List<Advertencia> validar(List<HoraVid> horas, Resultado r) {
        List<Advertencia> out = new ArrayList<>();

        HoraVid previa = null;
        Map<Long, ModuloHora> modAnterior = new HashMap<>();
        for (HoraVid h : horas) {
            boolean almuerzoTotal = h.todoAlmuerzo();

            long min = h.minutos();
            if (!almuerzoTotal && (min < MIN_NORMAL || min > MAX_NORMAL)) {
                out.add(new Advertencia(h.numero(), null, DURACION_ATIPICA,
                        "La hora %d dura %d min (%s a %s); lo normal es 60. Verifica el horario o normaliza los jabas/hora."
                                .formatted(h.numero(), min, h.inicio().format(HM), h.fin().format(HM))));
            }
            if (previa != null && !previa.fin().equals(h.inicio())) {
                out.add(new Advertencia(h.numero(), null, HUECO_HORARIO,
                        "La hora %d empieza a las %s pero la anterior termino a las %s."
                                .formatted(h.numero(), h.inicio().format(HM), previa.fin().format(HM))));
            }

            if (h.totalAjustado() != null && h.totalAjustado().compareTo(h.totalCalculado()) != 0) {
                out.add(new Advertencia(h.numero(), null, TOTAL_AJUSTADO,
                        "El total de la hora %d fue ajustado a mano (%s) y no coincide con la suma de los modulos (%s)."
                                .formatted(h.numero(), h.totalAjustado().toPlainString(), h.totalCalculado().toPlainString())));
            }

            for (ModuloHora m : h.modulos()) {
                boolean sinObs = m.observacion() == null || m.observacion().isBlank();
                if (!m.almuerzo()) {
                    if (m.jabasNz().signum() > 0 && m.personas() <= 0) {
                        out.add(new Advertencia(h.numero(), m.modulo(), MODULO_SIN_PERSONAS,
                                "%s registra jabas pero 0 personas en la hora %d.".formatted(m.modulo(), h.numero())));
                    }
                    if (m.personas() > 0 && m.jabasNz().signum() == 0 && sinObs) {
                        out.add(new Advertencia(h.numero(), m.modulo(), PERSONAS_SIN_JABAS,
                                "%s tiene %d personas pero 0 jabas en la hora %d y sin observacion (¿paro por falta de MP?)."
                                        .formatted(m.modulo(), m.personas(), h.numero())));
                    }
                }

                ModuloHora ant = m.maquinaId() == null ? null : modAnterior.get(m.maquinaId());
                if (ant != null && previa != null && m.jabasNz().signum() > 0
                        && m.jabasNz().compareTo(ant.jabasNz()) == 0) {
                    long difDuracion = Math.abs(h.minutos() - previa.minutos());
                    boolean tiempoParcial = declaraMinutos(m.observacion()) || declaraMinutos(ant.observacion());
                    if (difDuracion > TOLERANCIA_MINUTOS || tiempoParcial) {
                        out.add(new Advertencia(h.numero(), m.modulo(), VALOR_REPETIDO,
                                "%s repite %s jabas en las horas %d y %d con tiempos de operacion distintos."
                                        .formatted(m.modulo(), m.jabasNz().toPlainString(), previa.numero(), h.numero())));
                    }
                }
            }

            modAnterior.clear();
            for (ModuloHora m : h.modulos()) {
                if (m.maquinaId() != null) {
                    modAnterior.put(m.maquinaId(), m);
                }
            }
            previa = h;
        }

        if (r.totalPorDia().compareTo(r.procesadoDia()) != 0) {
            out.add(new Advertencia(null, null, DIA_DESCUADRADO,
                    "El procesado por dia suma %s pero el total por horas es %s."
                            .formatted(r.totalPorDia().toPlainString(), r.procesadoDia().toPlainString())));
        }
        if (r.totalSupervisores().compareTo(r.procesadoDia()) != 0) {
            out.add(new Advertencia(null, null, SUPERVISOR_DESCUADRADO,
                    "El resumen por supervisor suma %s pero el total por horas es %s."
                            .formatted(r.totalSupervisores().toPlainString(), r.procesadoDia().toPlainString())));
        }
        if (r.stock().signum() < 0) {
            out.add(new Advertencia(null, null, STOCK_NEGATIVO,
                    "Se proceso mas de lo ingresado: el stock queda en %s jabas. Revisa los ingresos de materia prima."
                            .formatted(r.stock().toPlainString())));
        }
        return out;
    }

    private static boolean declaraMinutos(String obs) {
        return obs != null && MINUTOS.matcher(obs).find();
    }
}
