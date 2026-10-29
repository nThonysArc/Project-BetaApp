package pe.betaagroindustrial.avance.reporte.vivo;

import pe.betaagroindustrial.avance.reporte.ReporteDatos;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.CorteReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.FilaReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.LoteReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.MaquinaCorteReporte;
import pe.betaagroindustrial.avance.reporte.vivo.AvanceVivo.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte {@link ReporteDatos} en {@link AvanceVivo}. Replica las reglas del renderer de la imagen
 * (acumulado por hora, pie de totales, resumen por maquina, barra de almuerzo) sin dibujar nada.
 * Sin estado ni dependencias: se prueba con datos en memoria.
 */
public final class AvanceVivoCalculador {

    private AvanceVivoCalculador() {
    }

    public static AvanceVivo calcular(ReporteDatos d) {
        // ---- Materia prima a procesar ----
        List<Ingreso> ingresos = new ArrayList<>();
        BigDecimal mpJabas = BigDecimal.ZERO;
        BigDecimal mpKilos = BigDecimal.ZERO;
        for (LoteReporte l : d.ingresos()) {
            ingresos.add(new Ingreso(l.variedad(), nz(l.jabas()), nz(l.kilos())));
            mpJabas = mpJabas.add(nz(l.jabas()));
            mpKilos = mpKilos.add(nz(l.kilos()));
        }

        // ---- Horas ----
        List<Hora> horas = new ArrayList<>();
        Map<Long, Acumulado> resumen = new LinkedHashMap<>();
        BigDecimal totJabas = BigDecimal.ZERO;
        BigDecimal totKilos = BigDecimal.ZERO;
        BigDecimal ultJabas = BigDecimal.ZERO;
        BigDecimal ultKilos = BigDecimal.ZERO;
        BigDecimal acumuladoKilos = BigDecimal.ZERO;
        int numero = 0;

        for (CorteReporte c : d.cortes()) {
            totJabas = totJabas.add(nz(c.jabasTotal()));
            totKilos = totKilos.add(nz(c.pesoTotal()));

            if (c.todoAlmuerzo()) {
                horas.add(new Hora(null, true, c.inicio(), c.fin(), c.fechaCosecha(), c.observacionGeneral(),
                        BigDecimal.ZERO, BigDecimal.ZERO, acumuladoKilos, List.of()));
                continue;
            }

            // "TOTAL" del pie = ultima hora de produccion (la barra de almuerzo no cuenta)
            ultJabas = nz(c.jabasTotal());
            ultKilos = nz(c.pesoTotal());
            acumuladoKilos = acumuladoKilos.add(nz(c.pesoTotal()));

            Map<Long, MaquinaCorteReporte> info = new HashMap<>();
            for (MaquinaCorteReporte m : c.maquinas()) {
                info.put(m.maquinaId(), m);
            }

            Map<Long, List<FilaReporte>> porMaquina = new LinkedHashMap<>();
            for (FilaReporte f : c.filas()) {
                porMaquina.computeIfAbsent(f.maquinaId(), id -> new ArrayList<>()).add(f);
            }

            List<MaquinaHora> maquinas = new ArrayList<>();
            for (Map.Entry<Long, List<FilaReporte>> e : porMaquina.entrySet()) {
                List<FilaReporte> filasMaquina = e.getValue();
                MaquinaCorteReporte m = info.get(e.getKey());
                BigDecimal jabas = BigDecimal.ZERO;
                BigDecimal kilos = BigDecimal.ZERO;
                List<Fila> filas = new ArrayList<>();
                for (FilaReporte f : filasMaquina) {
                    jabas = jabas.add(nz(f.jabas()));
                    kilos = kilos.add(nz(f.peso()));
                    filas.add(new Fila(f.kato(), f.variedad(), nz(f.jabas()), nz(f.peso()),
                            f.empacadores(), nz(f.kgPorEmpacador())));
                }
                String supervisor = m != null && !vacio(m.supervisor()) ? m.supervisor() : null;
                maquinas.add(new MaquinaHora(e.getKey(), filasMaquina.get(0).maquina(), supervisor,
                        m != null ? m.observacion() : null, m != null && m.almuerzo(), jabas, kilos, filas));

                // Resumen del dia por maquina (+ tramos de supervisor, igual que la imagen)
                Acumulado r = resumen.computeIfAbsent(e.getKey(), id -> new Acumulado(filasMaquina.get(0).maquina()));
                r.jabas = r.jabas.add(jabas);
                r.kilos = r.kilos.add(kilos);
                String sup = supervisor != null ? supervisor : "-";
                Tramo ultimo = r.tramos.isEmpty() ? null : r.tramos.get(r.tramos.size() - 1);
                if (ultimo != null && ultimo.supervisor().equals(sup)) {
                    r.tramos.set(r.tramos.size() - 1, new Tramo(sup, ultimo.inicio(), c.fin()));
                } else {
                    r.tramos.add(new Tramo(sup, c.inicio(), c.fin()));
                }
            }

            horas.add(new Hora(++numero, false, c.inicio(), c.fin(), c.fechaCosecha(), c.observacionGeneral(),
                    nz(c.jabasTotal()), nz(c.pesoTotal()), acumuladoKilos, maquinas));
        }

        Pie pie = new Pie(ultJabas, ultKilos,
                totJabas.subtract(ultJabas), totKilos.subtract(ultKilos),
                totJabas, totKilos,
                mpJabas.subtract(totJabas), mpKilos.subtract(totKilos));

        List<ResumenMaquina> resumenMaquinas = new ArrayList<>();
        BigDecimal rj = BigDecimal.ZERO;
        BigDecimal rk = BigDecimal.ZERO;
        for (Map.Entry<Long, Acumulado> e : resumen.entrySet()) {
            Acumulado r = e.getValue();
            resumenMaquinas.add(new ResumenMaquina(e.getKey(), r.nombre, r.jabas, r.kilos, List.copyOf(r.tramos)));
            rj = rj.add(r.jabas);
            rk = rk.add(r.kilos);
        }

        return new AvanceVivo(d.titulo(), d.fechaProceso(), ingresos, mpJabas, mpKilos, horas, pie,
                resumenMaquinas, rj, rk);
    }

    private static final class Acumulado {
        final String nombre;
        BigDecimal jabas = BigDecimal.ZERO;
        BigDecimal kilos = BigDecimal.ZERO;
        final List<Tramo> tramos = new ArrayList<>();

        Acumulado(String nombre) {
            this.nombre = nombre;
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
