package pe.betaagroindustrial.avance.reporte.vivo;

import org.junit.jupiter.api.Test;
import pe.betaagroindustrial.avance.reporte.ReporteDatos;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.CorteReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.FilaReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.LoteReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.MaquinaCorteReporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AvanceVivoCalculadorTest {

    private static final LocalDate DIA = LocalDate.of(2026, 8, 21);

    private static BigDecimal n(double v) {
        return BigDecimal.valueOf(v);
    }

    private static FilaReporte fila(long maquina, String nombre, double jabas, double peso) {
        return new FilaReporte(maquina, nombre, "K1", "Sekoya Pop", n(jabas), n(peso), 2, n(peso / 2));
    }

    private static CorteReporte corte(int h, double jabas, double peso, List<FilaReporte> filas,
                                      List<MaquinaCorteReporte> maquinas) {
        return new CorteReporte(LocalTime.of(h, 0), LocalTime.of(h + 1, 0), DIA, null, n(jabas), n(peso), filas, maquinas);
    }

    private static ReporteDatos datos(List<CorteReporte> cortes) {
        return new ReporteDatos("AVANCE", DIA, List.of(new LoteReporte(DIA, n(1000), n(2500), "Sekoya Pop")),
                cortes, null, null);
    }

    @Test
    void sinCortesSoloMuestraElStockCompleto() {
        AvanceVivo a = AvanceVivoCalculador.calcular(datos(List.of()));

        assertTrue(a.horas().isEmpty());
        assertEquals(0, n(1000).compareTo(a.pie().stockJabas()));
        assertEquals(0, n(2500).compareTo(a.pie().stockKilos()));
    }

    @Test
    void acumulaKilosHoraPorHoraYArmaElPie() {
        MaquinaCorteReporte m1 = new MaquinaCorteReporte(1L, "ANA", null, false);
        AvanceVivo a = AvanceVivoCalculador.calcular(datos(List.of(
                corte(8, 100, 250, List.of(fila(1, "Maquina 1", 100, 250)), List.of(m1)),
                corte(9, 120, 300, List.of(fila(1, "Maquina 1", 120, 300)), List.of(m1)))));

        assertEquals(2, a.horas().size());
        assertEquals(1, a.horas().get(0).numero());
        assertEquals(2, a.horas().get(1).numero());
        assertEquals(0, n(550).compareTo(a.horas().get(1).acumuladoKilos()));

        assertEquals(0, n(120).compareTo(a.pie().ultimaHoraJabas()));
        assertEquals(0, n(100).compareTo(a.pie().previoJabas()));
        assertEquals(0, n(220).compareTo(a.pie().acumuladoJabas()));
        assertEquals(0, n(780).compareTo(a.pie().stockJabas()));
        assertEquals(0, n(1950).compareTo(a.pie().stockKilos()));
    }

    @Test
    void laBarraDeAlmuerzoNoRecibeNumeroNiCambiaElTotalDeLaUltimaHora() {
        MaquinaCorteReporte comiendo = new MaquinaCorteReporte(1L, "ANA", null, true);
        MaquinaCorteReporte m1 = new MaquinaCorteReporte(1L, "ANA", null, false);
        AvanceVivo a = AvanceVivoCalculador.calcular(datos(List.of(
                corte(8, 100, 250, List.of(fila(1, "Maquina 1", 100, 250)), List.of(m1)),
                corte(9, 0, 0, List.of(fila(1, "Maquina 1", 0, 0)), List.of(comiendo)))));

        assertEquals(2, a.horas().size());
        assertTrue(a.horas().get(1).pausa());
        assertNull(a.horas().get(1).numero());
        assertEquals(0, n(100).compareTo(a.pie().ultimaHoraJabas()));
    }

    @Test
    void elResumenSumaPorMaquinaYAgrupaTramosDeSupervisor() {
        List<FilaReporte> f1 = List.of(fila(1, "Maquina 1", 100, 250));
        List<FilaReporte> f2 = List.of(fila(1, "Maquina 1", 50, 125));
        List<FilaReporte> f3 = List.of(fila(1, "Maquina 1", 70, 175));
        AvanceVivo a = AvanceVivoCalculador.calcular(datos(List.of(
                corte(8, 100, 250, f1, List.of(new MaquinaCorteReporte(1L, "ANA", null, false))),
                corte(9, 50, 125, f2, List.of(new MaquinaCorteReporte(1L, "ANA", null, false))),
                corte(10, 70, 175, f3, List.of(new MaquinaCorteReporte(1L, "LUIS", null, false))))));

        assertEquals(1, a.resumen().size());
        AvanceVivo.ResumenMaquina r = a.resumen().get(0);
        assertEquals(0, n(220).compareTo(r.jabas()));
        assertEquals(2, r.tramos().size());
        assertEquals("ANA", r.tramos().get(0).supervisor());
        assertEquals(LocalTime.of(10, 0), r.tramos().get(0).fin());
        assertEquals("LUIS", r.tramos().get(1).supervisor());
        assertEquals(0, n(220).compareTo(a.totalJabas()));
    }
}
