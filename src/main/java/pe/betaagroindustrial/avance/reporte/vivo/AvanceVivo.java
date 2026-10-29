package pe.betaagroindustrial.avance.reporte.vivo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Avance del dia en forma de datos (JSON) para mostrarlo en la web: los MISMOS numeros que dibuja la
 * imagen PNG del reporte de arandano, pero estructurados para que el frontend los pinte como tabla.
 * Es un modelo plano (sin JPA ni Spring): lo produce {@link AvanceVivoCalculador} a partir de
 * {@code ReporteDatos}, de modo que web e imagen jamas se contradicen.
 */
public record AvanceVivo(
        String titulo,
        LocalDate fecha,
        List<Ingreso> ingresos,
        BigDecimal ingresoJabas,
        BigDecimal ingresoKilos,
        List<Hora> horas,
        Pie pie,
        List<ResumenMaquina> resumen,
        BigDecimal totalJabas,
        BigDecimal totalKilos
) {

    /** Una fila de "TOTAL A PROCESAR". */
    public record Ingreso(String variedad, BigDecimal jabas, BigDecimal kilos) {
    }

    /**
     * Una hora del cuadro. {@code pausa}=true es la barra "ALMUERZO DEL PERSONAL" (todas las maquinas
     * almorzando): no es hora de produccion, no recibe numero ni suma.
     */
    public record Hora(
            Integer numero,
            boolean pausa,
            LocalTime inicio,
            LocalTime fin,
            LocalDate fechaCosecha,
            String observacionGeneral,
            BigDecimal jabas,
            BigDecimal kilos,
            BigDecimal acumuladoKilos,
            List<MaquinaHora> maquinas
    ) {
    }

    /** Lo que hizo una maquina en una hora (sus filas kato+variedad, supervisor y observacion). */
    public record MaquinaHora(
            Long maquinaId,
            String maquina,
            String supervisor,
            String observacion,
            boolean almuerzo,
            BigDecimal jabas,
            BigDecimal kilos,
            List<Fila> filas
    ) {
    }

    public record Fila(
            String kato,
            String variedad,
            BigDecimal jabas,
            BigDecimal peso,
            int empacadores,
            BigDecimal kgPorEmpacador
    ) {
    }

    /** Pie del cuadro: TOTAL (ultima hora), ACUMULADO (previo), TOTAL ACUMULADO y STOCK por procesar. */
    public record Pie(
            BigDecimal ultimaHoraJabas, BigDecimal ultimaHoraKilos,
            BigDecimal previoJabas, BigDecimal previoKilos,
            BigDecimal acumuladoJabas, BigDecimal acumuladoKilos,
            BigDecimal stockJabas, BigDecimal stockKilos
    ) {
    }

    /** Total por maquina en el dia, con los supervisores que tuvo (y en que tramo horario). */
    public record ResumenMaquina(
            Long maquinaId,
            String maquina,
            BigDecimal jabas,
            BigDecimal kilos,
            List<Tramo> tramos
    ) {
    }

    public record Tramo(String supervisor, LocalTime inicio, LocalTime fin) {
    }
}
