package pe.betaagroindustrial.avance.reporte;

import org.junit.jupiter.api.Test;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.CorteReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.FilaReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.LoteReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.MaquinaCorteReporte;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ReporteXlsxRendererTest {

    private static FilaReporte fila(long maq, String kato, String variedad, int jabas, String peso, int emp) {
        BigDecimal p = new BigDecimal(peso);
        return new FilaReporte(maq, "MAQUINA " + maq, kato, variedad, BigDecimal.valueOf(jabas), p, emp,
                p.divide(BigDecimal.valueOf(emp), 2, java.math.RoundingMode.HALF_UP));
    }

    private static ReporteDatos datos() {
        List<MaquinaCorteReporte> ms = List.of(
                new MaquinaCorteReporte(1L, "Juan Perez", "", false),
                new MaquinaCorteReporte(2L, "Ana Diaz", "Paro por limpieza", false));
        CorteReporte c1 = new CorteReporte(LocalTime.of(7, 0), LocalTime.of(8, 0), LocalDate.of(2026, 11, 4), null,
                BigDecimal.valueOf(1200), new BigDecimal("6000.0"),
                List.of(fila(1, "KATO 1", "VENTURA/SEKOYA", 600, "3000.0", 6), fila(2, "KATO 1", "VENTURA", 600, "3000.0", 6)), ms);
        CorteReporte c2 = new CorteReporte(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalDate.of(2026, 11, 4), "Proceso final",
                BigDecimal.valueOf(1000), new BigDecimal("5000.0"),
                List.of(fila(1, "KATO 1", "VENTURA", 500, "2500.0", 5), fila(2, "KATO 1", "VENTURA", 500, "2500.0", 5)), ms);
        return new ReporteDatos("AVANCE DE PRODUCCION CAMPAÑA ARANDANOS 2026", LocalDate.of(2026, 11, 4),
                List.of(new LoteReporte(LocalDate.of(2026, 11, 4), BigDecimal.valueOf(10000), new BigDecimal("50000.0"), "VENTURA")),
                List.of(c1, c2), null, null);
    }

    private static Map<String, String> descomprimir(byte[] xlsx) throws Exception {
        Map<String, String> partes = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                partes.put(e.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return partes;
    }

    private static void esXmlValido(String xml) throws Exception {
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        }
    }

    @Test
    void generaUnLibroValidoConLasPartesNecesarias() throws Exception {
        Map<String, String> partes = descomprimir(ReporteXlsxRenderer.renderizar(datos()));

        assertThat(partes).containsKeys("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels", "xl/styles.xml", "xl/worksheets/sheet1.xml");
        for (String xml : partes.values()) {
            esXmlValido(xml);
        }
    }

    @Test
    void losTotalesSonFormulasYLosDatosBaseSonValores() throws Exception {
        String hoja = descomprimir(ReporteXlsxRenderer.renderizar(datos())).get("xl/worksheets/sheet1.xml");

        assertThat(hoja).contains("<f>SUM(");          // peso por maquina, total por hora, totales
        assertThat(hoja).contains("<f>SUMIF(");        // resumen por maquina
        assertThat(hoja).contains("<f>IF(");           // kg por empacador
        assertThat(hoja).contains("<mergeCell ");      // celdas combinadas como en la imagen
        assertThat(hoja).contains("PROCESO FINAL");    // observacion general en una sola celda
        assertThat(hoja).contains("VENTURA/SEKOYA");   // variedades fusionadas por kato
    }

    @Test
    void sinCortesNiIngresosTambienGeneraElLibro() throws Exception {
        ReporteDatos vacio = new ReporteDatos("AVANCE", LocalDate.of(2026, 11, 4), List.of(), List.of(), null, null);
        Map<String, String> partes = descomprimir(ReporteXlsxRenderer.renderizar(vacio));
        esXmlValido(partes.get("xl/worksheets/sheet1.xml"));
        assertThat(partes.get("xl/worksheets/sheet1.xml")).contains("SIN CORTES REGISTRADOS");
    }

    @Test
    void sinCeldasCombinadasQueSeSolapen() throws Exception {
        String hoja = descomprimir(ReporteXlsxRenderer.renderizar(datos())).get("xl/worksheets/sheet1.xml");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<mergeCell ref=\"([A-Z]+)(\\d+):([A-Z]+)(\\d+)\"/>").matcher(hoja);
        List<int[]> rangos = new java.util.ArrayList<>();
        while (m.find()) {
            rangos.add(new int[]{col(m.group(1)), Integer.parseInt(m.group(2)), col(m.group(3)), Integer.parseInt(m.group(4))});
        }
        for (int i = 0; i < rangos.size(); i++) {
            for (int j = i + 1; j < rangos.size(); j++) {
                int[] a = rangos.get(i), b = rangos.get(j);
                boolean separados = a[2] < b[0] || b[2] < a[0] || a[3] < b[1] || b[3] < a[1];
                assertThat(separados).as("rangos %d y %d", i, j).isTrue();
            }
        }
    }

    private static int col(String letras) {
        int c = 0;
        for (char ch : letras.toCharArray()) {
            c = c * 26 + (ch - 'A' + 1);
        }
        return c;
    }
}
