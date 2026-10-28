package pe.betaagroindustrial.avance.reporte.vid;

import org.junit.jupiter.api.Test;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.Contexto;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.HoraVid;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.betaagroindustrial.avance.reporte.vid.DatosVid.*;

class ValidadorAvanceVidTest {

    private static List<Advertencia> validar(List<HoraVid> horas) {
        // Ingreso holgado para que el stock no genere ruido en pruebas que miran otra regla.
        Contexto ctx = new Contexto(D28, BigDecimal.valueOf(1_000_000), BigDecimal.ZERO, BigDecimal.ZERO);
        return ValidadorAvanceVid.validar(horas, AvanceVid.calcular(horas, ctx));
    }

    private static long cuantas(List<Advertencia> adv, String codigo) {
        return adv.stream().filter(a -> a.codigo().equals(codigo)).count();
    }

    @Test
    void unDiaLimpioNoGeneraAdvertencias() {
        List<HoraVid> horas = List.of(
                hora(1, t(8, 0), t(9, 0), D28, modulo(1, 10, 100, "ANA", null)),
                hora(2, t(9, 0), t(10, 0), D28, modulo(1, 10, 110, "ANA", null)));

        assertTrue(validar(horas).isEmpty());
    }

    @Test
    void unaHoraDe70MinutosSeMarca() {
        List<HoraVid> horas = List.of(hora(1, t(8, 50), t(10, 0), D28, modulo(1, 10, 100, "ANA", null)));

        assertEquals(1, cuantas(validar(horas), ValidadorAvanceVid.DURACION_ATIPICA));
    }

    @Test
    void unaHoraDe60MinutosYUnaDe65NoSeMarcan() {
        List<HoraVid> horas = List.of(
                hora(1, t(8, 0), t(9, 0), D28, modulo(1, 10, 100, "ANA", null)),
                hora(2, t(9, 0), t(10, 5), D28, modulo(1, 10, 110, "ANA", null)));

        assertEquals(0, cuantas(validar(horas), ValidadorAvanceVid.DURACION_ATIPICA));
    }

    @Test
    void laHoraDeAlmuerzoDelPersonalNoSeMarcaPorDuracion() {
        List<HoraVid> horas = List.of(hora(1, t(12, 0), t(14, 0), D28, almuerzo(1, 10), almuerzo(2, 10)));

        assertEquals(0, cuantas(validar(horas), ValidadorAvanceVid.DURACION_ATIPICA));
    }

    @Test
    void unHuecoEntreHorasSeMarca() {
        List<HoraVid> horas = List.of(
                hora(1, t(8, 0), t(9, 0), D28, modulo(1, 10, 100, "ANA", null)),
                hora(2, t(10, 0), t(11, 0), D28, modulo(1, 10, 110, "ANA", null)));

        assertEquals(1, cuantas(validar(horas), ValidadorAvanceVid.HUECO_HORARIO));
    }

    @Test
    void elMismoValorConTiempoParcialDeclaradoSeMarcaComoRepetido() {
        // Caso real: M5 con 160 jabas en la hora 1 y de nuevo 160 en una hora de solo "10 mnts de proceso".
        List<HoraVid> horas = List.of(
                hora(1, t(8, 0), t(9, 0), D28, modulo(5, 8, 160, "ANA", null)),
                hora(2, t(9, 0), t(10, 0), D28, modulo(5, 8, 160, "ANA", "10 mnts de proceso")));

        List<Advertencia> adv = validar(horas);

        assertEquals(1, cuantas(adv, ValidadorAvanceVid.VALOR_REPETIDO));
        assertEquals("MODULO 5", adv.get(0).modulo());
    }

    @Test
    void elMismoValorEnHorasDeIgualDuracionSinObservacionNoSeMarca() {
        List<HoraVid> horas = List.of(
                hora(1, t(8, 0), t(9, 0), D28, modulo(1, 10, 100, "ANA", null)),
                hora(2, t(9, 0), t(10, 0), D28, modulo(1, 10, 100, "ANA", null)));

        assertEquals(0, cuantas(validar(horas), ValidadorAvanceVid.VALOR_REPETIDO));
    }

    @Test
    void jabasSinPersonasYPersonasSinJabasSeMarcan() {
        List<HoraVid> horas = List.of(hora(1, t(8, 0), t(9, 0), D28,
                modulo(1, 0, 100, "ANA", null), modulo(2, 6, 0, "LUIS", null)));

        List<Advertencia> adv = validar(horas);

        assertEquals(1, cuantas(adv, ValidadorAvanceVid.MODULO_SIN_PERSONAS));
        assertEquals(1, cuantas(adv, ValidadorAvanceVid.PERSONAS_SIN_JABAS));
    }

    @Test
    void personasSinJabasConObservacionDeParoNoSeMarca() {
        List<HoraVid> horas = List.of(hora(1, t(8, 0), t(9, 0), D28,
                modulo(1, 6, 0, "ANA", "Paro por falta de MP")));

        assertEquals(0, cuantas(validar(horas), ValidadorAvanceVid.PERSONAS_SIN_JABAS));
    }

    @Test
    void unModuloEnAlmuerzoNoGeneraAdvertenciasDePersonas() {
        List<HoraVid> horas = List.of(hora(1, t(8, 0), t(9, 0), D28, almuerzo(1, 10), modulo(2, 5, 50, "LUIS", null)));

        List<Advertencia> adv = validar(horas);

        assertEquals(0, cuantas(adv, ValidadorAvanceVid.PERSONAS_SIN_JABAS));
    }

    @Test
    void unTotalAjustadoADistintoDeLaSumaDescuadraLaHoraYElDia() {
        HoraVid ajustada = new HoraVid(1, t(8, 0), t(9, 0), D28, BigDecimal.valueOf(90),
                List.of(modulo(1, 10, 100, "ANA", null)));

        List<Advertencia> adv = validar(List.of(ajustada));

        assertEquals(1, cuantas(adv, ValidadorAvanceVid.TOTAL_AJUSTADO));
        assertEquals(1, cuantas(adv, ValidadorAvanceVid.DIA_DESCUADRADO));
        assertEquals(1, cuantas(adv, ValidadorAvanceVid.SUPERVISOR_DESCUADRADO));
    }

    @Test
    void procesarMasDeLoIngresadoDejaStockNegativo() {
        List<HoraVid> horas = List.of(hora(1, t(8, 0), t(9, 0), D28, modulo(1, 10, 100, "ANA", null)));
        Contexto ctx = new Contexto(D28, BigDecimal.valueOf(50), BigDecimal.ZERO, BigDecimal.ZERO);

        List<Advertencia> adv = ValidadorAvanceVid.validar(horas, AvanceVid.calcular(horas, ctx));

        assertEquals(1, cuantas(adv, ValidadorAvanceVid.STOCK_NEGATIVO));
    }
}
