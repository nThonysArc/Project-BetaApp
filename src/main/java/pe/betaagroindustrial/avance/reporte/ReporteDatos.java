package pe.betaagroindustrial.avance.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos ya preparados para dibujar el reporte de avance. Es un modelo plano
 * (sin JPA ni Spring) para que el dibujo no dependa de las entidades y se
 * pueda probar de forma aislada.
 */
public record ReporteDatos(
        String titulo,
        LocalDate fechaProceso,
        List<LoteReporte> ingresos,
        List<CorteReporte> cortes,
        byte[] logo,
        byte[] fotoProducto
) {

    /** Una fila de "TOTAL A PROCESAR": materia prima reportada por variedad. */
    public record LoteReporte(LocalDate fecha, BigDecimal jabas, BigDecimal kilos, String variedad) {
    }

    /** Una fila del detalle: maquina + kato + variedad. */
    public record FilaReporte(
            Long maquinaId,
            String maquina,
            String kato,
            String variedad,
            BigDecimal jabas,
            BigDecimal peso,
            int empacadores,
            BigDecimal kgPorEmpacador
    ) {
    }

    /** Supervisor y observacion de una maquina en un corte (supervisor/observacion pueden ser null). */
    public record MaquinaCorteReporte(Long maquinaId, String supervisor, String observacion, boolean almuerzo) {
    }

    /** Un corte (una hora). jabasTotal/pesoTotal son los efectivos (con ajuste manual si lo hay). */
    public record CorteReporte(
            LocalTime inicio,
            LocalTime fin,
            LocalDate fechaCosecha,
            String observacionGeneral,
            BigDecimal jabasTotal,
            BigDecimal pesoTotal,
            List<FilaReporte> filas,
            List<MaquinaCorteReporte> maquinas
    ) {
        /**
         * Todas las maquinas del corte salieron a almorzar: no es una hora de produccion, el reporte
         * dibuja la barra "ALMUERZO DEL PERSONAL" (y la hora no recibe numero).
         */
        public boolean todoAlmuerzo() {
            return maquinas != null && !maquinas.isEmpty() && maquinas.stream().allMatch(MaquinaCorteReporte::almuerzo);
        }
    }
}
