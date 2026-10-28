package pe.betaagroindustrial.avance.reporte.vid;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.betaagroindustrial.avance.reporte.vid.DatosVid.*;

class ReporteVidRendererTest {

    @Test
    void generaUnPngValidoQueCreceConCadaHora() {
        List<AvanceVid.HoraVid> una = List.of(hora(1, t(8, 0), t(9, 0), D28, modulo(1, 10, 100, "ANA", null)));
        List<AvanceVid.HoraVid> dos = List.of(una.get(0),
                hora(2, t(9, 0), t(10, 0), D28, modulo(1, 10, 110, "ANA", null)));

        byte[] png1 = png(una);
        byte[] png2 = png(dos);

        // Firma PNG: 0x89 'P' 'N' 'G'
        assertEquals((byte) 0x89, png1[0]);
        assertEquals((byte) 'P', png1[1]);
        assertTrue(png2.length > png1.length, "con mas horas la imagen debe ser mas grande");
    }

    @Test
    void lasHorasSeMuestranEnVeinticuatroHoras() {
        assertEquals("08:50\nA\n09:50", ReporteVidRenderer.rangoHora(t(8, 50), t(9, 50)));
        assertEquals("14:15\nA\n15:15", ReporteVidRenderer.rangoHora(t(14, 15), t(15, 15)));
        assertEquals("00:15\nA\n01:15", ReporteVidRenderer.rangoHora(t(0, 15), t(1, 15)));
    }

    @Test
    void hayArrastreSoloSiSeProcesaFrutaDeMasDeUnDiaDeCosecha() {
        AvanceVid.Contexto ctx = new AvanceVid.Contexto(D28, BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.ZERO);

        AvanceVid.Resultado mismoDia = AvanceVid.calcular(List.of(
                hora(1, t(8, 0), t(9, 0), D28, modulo(1, 10, 100, "ANA", null))), ctx);
        AvanceVid.Resultado variosDias = AvanceVid.calcular(List.of(
                hora(1, t(8, 0), t(9, 0), D27, modulo(1, 10, 100, "ANA", null)),
                hora(2, t(9, 0), t(10, 0), D28, modulo(1, 10, 110, "ANA", null))), ctx);

        assertTrue(!ReporteVidRenderer.conArrastre(mismoDia));
        assertTrue(ReporteVidRenderer.conArrastre(variosDias));
    }

    private static byte[] png(List<AvanceVid.HoraVid> horas) {
        AvanceVid.Contexto ctx = new AvanceVid.Contexto(D28, BigDecimal.valueOf(1000), BigDecimal.ZERO, BigDecimal.ZERO);
        AvanceVid.Resultado r = AvanceVid.calcular(horas, ctx);
        return ReporteVidRenderer.renderizarPng(
                new ReporteVidRenderer.Entrada("AVANCE POR HORA DE SELECCIÓN VID 2025", D28, r, null, null, null), 1.0);
    }
}
