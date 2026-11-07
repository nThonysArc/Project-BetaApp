package pe.betaagroindustrial.avance.reporte.vid;

import org.junit.jupiter.api.Test;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.Contexto;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.HoraVid;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.Resultado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static pe.betaagroindustrial.avance.reporte.vid.DatosVid.*;

class AvanceVidTest {

    private static BigDecimal n(long v) {
        return BigDecimal.valueOf(v);
    }

    private static Contexto ctx(long ingresoDia, long ingresoAnterior, long acumuladoAnterior) {
        return new Contexto(D28, n(ingresoDia), n(ingresoAnterior), n(acumuladoAnterior));
    }

    private final List<HoraVid> dosHoras = List.of(
            hora(1, t(8, 0), t(9, 0), D27, modulo(1, 10, 100, "ANA", null), modulo(2, 5, 50, "LUIS", null)),
            hora(2, t(9, 0), t(10, 0), D28, modulo(1, 10, 120, "ANA", null), modulo(2, 5, 30, "LUIS", null)));

    @Test
    void acumuladoCrecePorHoraYElTotalCoincideConLaSumaDeModulos() {
        Resultado r = AvanceVid.calcular(dosHoras, ctx(0, 0, 0));

        assertEquals(n(150), r.bloques().get(0).totalHora());
        assertEquals(n(150), r.bloques().get(0).acumulado());
        assertEquals(n(150), r.bloques().get(1).totalHora());
        assertEquals(n(300), r.bloques().get(1).acumulado());
        assertEquals(n(300), r.procesadoDia());
    }

    @Test
    void rendimientoSiempreEsJabasEntrePersonas() {
        Resultado r = AvanceVid.calcular(dosHoras, ctx(0, 0, 0));

        assertEquals(n(10), r.bloques().get(0).filas().get(0).rendimiento());
        assertEquals(n(10), r.bloques().get(0).filas().get(1).rendimiento());
        assertEquals(n(12), r.bloques().get(1).filas().get(0).rendimiento());
        assertEquals(n(6), r.bloques().get(1).filas().get(1).rendimiento());
    }

    @Test
    void sinPersonasNoHayRendimiento() {
        List<HoraVid> horas = List.of(hora(1, t(8, 0), t(9, 0), D28, modulo(1, 0, 40, "ANA", null)));

        assertNull(AvanceVid.calcular(horas, ctx(0, 0, 0)).bloques().get(0).filas().get(0).rendimiento());
    }

    @Test
    void procesadoPorDiaSeAgrupaPorFechaDeCosechaYSumaElTotal() {
        Resultado r = AvanceVid.calcular(dosHoras, ctx(0, 0, 0));

        assertEquals(2, r.porDia().size());
        assertEquals(D27, r.porDia().get(0).fecha());
        assertEquals(n(150), r.porDia().get(0).jabas());
        assertEquals(n(150), r.porDia().get(1).jabas());
        assertEquals(r.procesadoDia(), r.totalPorDia());
    }

    @Test
    void elResumenPorSupervisorIncluyeTodosLosModulosYCuadraConElTotal() {
        // El Excel original omitia el ultimo modulo y el resumen no sumaba el total.
        List<HoraVid> horas = List.of(hora(1, t(8, 0), t(9, 0), D28,
                modulo(1, 10, 100, "ANA", null), modulo(2, 5, 50, "LUIS", null), modulo(5, 4, 320, "LUIS", null)));

        Resultado r = AvanceVid.calcular(horas, ctx(0, 0, 0));

        assertEquals(3, r.supervisores().size());
        assertEquals(n(470), r.totalSupervisores());
        assertEquals(r.procesadoDia(), r.totalSupervisores());
    }

    @Test
    void unModuloSinSupervisorApareceComoSinSupervisorEnVezDeDesaparecer() {
        List<HoraVid> horas = List.of(hora(1, t(8, 0), t(9, 0), D28, modulo(1, 10, 100, null, null)));

        assertEquals(AvanceVid.SIN_SUPERVISOR, AvanceVid.calcular(horas, ctx(0, 0, 0)).supervisores().get(0).supervisor());
    }

    @Test
    void lasVariedadesDeUnModuloSeUnenConBarra() {
        List<HoraVid> horas = List.of(
                hora(1, t(8, 0), t(9, 0), D28, new AvanceVid.ModuloHora(1L, "MODULO 1", "Sweet Globe", 10, n(100), "ANA", null, false)),
                hora(2, t(9, 0), t(10, 0), D28, new AvanceVid.ModuloHora(1L, "MODULO 1", "Timco", 10, n(100), "ANA", null, false)));

        assertEquals("Sweet Globe/Timco", AvanceVid.calcular(horas, ctx(0, 0, 0)).supervisores().get(0).variedades());
    }

    @Test
    void totalAcumuladoYStockUsanLosDiasAnterioresDeLaCampana() {
        // ingreso del dia 1000 + 500 de dias previos; procesado hoy 300 + 200 previo.
        Resultado r = AvanceVid.calcular(dosHoras, ctx(1000, 500, 200));

        assertEquals(n(1000), r.proyeccion());
        assertEquals(n(200), r.acumuladoAnterior());
        assertEquals(n(500), r.totalAcumulado());
        assertEquals(n(1000), r.stock()); // 500 + 1000 - 500
    }

    @Test
    void laHoraEnQueTodoElPersonalAlmuerzaNoOcupaNumeroYLaSiguienteContinuaLaCuenta() {
        List<HoraVid> horas = AvanceVid.numerar(List.of(
                hora(1, t(8, 0), t(9, 0), D28, modulo(1, 10, 100, "ANA", null)),
                hora(2, t(9, 0), t(10, 0), D28, almuerzo(1, 10), almuerzo(2, 10)),
                hora(3, t(10, 0), t(11, 0), D28, modulo(1, 10, 120, "ANA", null))));

        assertEquals(List.of(1, 0, 2), horas.stream().map(HoraVid::numero).toList());
    }

    @Test
    void unModuloEnAlmuerzoNoHaceDeLaHoraUnaBarraPeroSiLaHoraEntera() {
        Resultado r = AvanceVid.calcular(List.of(
                hora(1, t(8, 0), t(9, 0), D28, almuerzo(1, 10), modulo(2, 10, 100, "ANA", null)),
                hora(2, t(9, 0), t(10, 0), D28, almuerzo(1, 10), almuerzo(2, 10))), ctx(1000, 0, 0));

        assertEquals(false, r.bloques().get(0).todoAlmuerzo());
        assertEquals(true, r.bloques().get(1).todoAlmuerzo());
        assertEquals(n(100), r.procesadoDia()); // el almuerzo no suma produccion
    }

    @Test
    void elStockDigitadoAManoReemplazaAlDeducidoDelHistorial() {
        // El historial diria 500 - 200 = 300, pero el supervisor digito 700 (del 03/11): manda lo digitado.
        LocalDate fecha = LocalDate.of(2026, 11, 4);
        Contexto c = new Contexto(fecha, n(1000), n(500), n(200), null, n(700), fecha.minusDays(1));
        Resultado r = AvanceVid.calcular(dosHoras, c);

        assertEquals(n(700), r.stockAnterior());
        assertEquals(fecha.minusDays(1), r.stockAnteriorFecha());
        assertEquals(n(1000), r.ingresoDia());
        assertEquals(n(1400), r.stock()); // 700 + 1000 - 300 procesado hoy
    }

    @Test
    void sinStockDigitadoSeDeduceDelHistorialYNoLlevaFecha() {
        Resultado r = AvanceVid.calcular(dosHoras, ctx(1000, 500, 200));

        assertEquals(n(300), r.stockAnterior());
        assertNull(r.stockAnteriorFecha());
    }

    @Test
    void elStockDeDiasPreviosQuedaSeparadoDelIngresoDelDia() {
        // Ayer ingresaron 500 y se procesaron 200: quedan 300 de stock. Hoy ingresan 1000 y se procesan 300.
        Resultado r = AvanceVid.calcular(dosHoras, ctx(1000, 500, 200));

        assertEquals(n(300), r.stockAnterior());
        assertEquals(n(1000), r.ingresoDia());
        assertEquals(r.stock(), r.stockAnterior().add(r.ingresoDia()).subtract(r.procesadoDia()));
    }

    @Test
    void unTotalAjustadoManualMandaSobreLaSumaDeModulos() {
        HoraVid ajustada = new HoraVid(1, t(8, 0), t(9, 0), D28, n(999), List.of(modulo(1, 10, 100, "ANA", null)));

        Resultado r = AvanceVid.calcular(List.of(ajustada), ctx(0, 0, 0));

        assertEquals(n(999), r.procesadoDia());
        assertEquals(n(100), r.totalSupervisores());
    }

    @Test
    void laProyeccionDigitadaSeMuestraAparteDelIngreso() {
        Contexto ctx = new Contexto(D28, n(4529), BigDecimal.ZERO, BigDecimal.ZERO, n(4000));
        Resultado r = AvanceVid.calcular(List.of(), ctx);
        assertEquals(0, n(4000).compareTo(r.proyeccion()), "la proyeccion es la digitada, no el ingreso");
        assertEquals(0, n(4529).compareTo(r.stock()), "el stock sigue saliendo del ingreso real");
    }

    @Test
    void sinProyeccionDigitadaSeUsaElIngresoDelDia() {
        Contexto ctx = new Contexto(D28, n(4529), BigDecimal.ZERO, BigDecimal.ZERO);
        Resultado r = AvanceVid.calcular(List.of(), ctx);
        assertEquals(0, n(4529).compareTo(r.proyeccion()));
    }
}
