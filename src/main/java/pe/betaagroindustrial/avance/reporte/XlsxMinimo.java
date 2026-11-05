package pe.betaagroindustrial.avance.reporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Escritor minimo de libros .xlsx (Office Open XML) SIN dependencias externas: una sola hoja con
 * estilos, celdas combinadas, formulas (con su valor en cache), anchos/altos y imagenes PNG.
 *
 * Por que no Apache POI: el reporte necesita pocas capacidades y asi el backend no suma ~10 MB de
 * librerias ni tiempo de arranque. Filas y columnas son base 0 en esta API.
 */
final class XlsxMinimo {

    /** Formato de una celda. Colores en hex RRGGBB; null = sin relleno / sin borde. */
    record Estilo(double pt, boolean negrita, String color, String relleno, String borde,
                  String horizontal, String vertical, boolean ajustar, String formato) {
    }

    private record Celda(char tipo, String texto, double numero, String formula, int estilo) {
    }

    private record Imagen(byte[] png, int col, int fila, double offXpx, double offYpx, double wpx, double hpx) {
    }

    private final String nombreHoja;
    private final Map<Estilo, Integer> estilos = new LinkedHashMap<>();
    private final TreeMap<Integer, TreeMap<Integer, Celda>> celdas = new TreeMap<>();
    private final Map<Integer, Double> altos = new TreeMap<>();
    private final Map<Integer, Double> anchos = new TreeMap<>();
    private final List<int[]> combinadas = new ArrayList<>();
    private final List<Imagen> imagenes = new ArrayList<>();
    private int ultimaCol = 0;

    XlsxMinimo(String nombreHoja) {
        this.nombreHoja = nombreHoja;
        estilo(new Estilo(11, false, "000000", null, null, null, null, false, null)); // estilo 0 = por defecto
    }

    // ------------------------------------------------------------------ API

    int estilo(Estilo e) {
        return estilos.computeIfAbsent(e, k -> estilos.size());
    }

    void texto(int fila, int col, String s, int estilo) {
        poner(fila, col, new Celda('s', s, 0, null, estilo));
    }

    void numero(int fila, int col, double v, int estilo) {
        poner(fila, col, new Celda('n', null, v, null, estilo));
    }

    /** Formula (sin "=") con su resultado en cache, para que se vea bien aun antes de recalcular. */
    void formula(int fila, int col, String formula, double cacheado, int estilo) {
        poner(fila, col, new Celda('f', null, cacheado, formula, estilo));
    }

    void vacia(int fila, int col, int estilo) {
        poner(fila, col, new Celda('b', null, 0, null, estilo));
    }

    void altoFila(int fila, double pt) {
        altos.put(fila, pt);
    }

    void anchoCol(int col, double caracteres) {
        anchos.put(col, caracteres);
        ultimaCol = Math.max(ultimaCol, col);
    }

    /** Combina el rango y aplica el estilo a todas sus celdas (los bordes se ven en toda la celda combinada). */
    void combinar(int f1, int c1, int f2, int c2, int estilo) {
        for (int f = f1; f <= f2; f++) {
            for (int c = c1; c <= c2; c++) {
                if (!celdas.containsKey(f) || !celdas.get(f).containsKey(c)) {
                    vacia(f, c, estilo);
                }
            }
        }
        if (f1 != f2 || c1 != c2) {
            combinadas.add(new int[]{f1, c1, f2, c2});
        }
    }

    /** Imagen PNG anclada a una celda, con desplazamiento y tamano en pixeles. */
    void imagen(byte[] png, int col, int fila, double offXpx, double offYpx, double wpx, double hpx) {
        if (png != null && png.length > 0) {
            imagenes.add(new Imagen(png, col, fila, offXpx, offYpx, wpx, hpx));
        }
    }

    // ------------------------------------------------------------------ generacion

    byte[] generar() {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(salida)) {
            entrada(zip, "[Content_Types].xml", contentTypes());
            entrada(zip, "_rels/.rels", relsRaiz());
            entrada(zip, "xl/workbook.xml", workbook());
            entrada(zip, "xl/_rels/workbook.xml.rels", relsLibro());
            entrada(zip, "xl/styles.xml", styles());
            entrada(zip, "xl/worksheets/sheet1.xml", hoja());
            if (!imagenes.isEmpty()) {
                entrada(zip, "xl/worksheets/_rels/sheet1.xml.rels", relsHoja());
                entrada(zip, "xl/drawings/drawing1.xml", dibujo());
                entrada(zip, "xl/drawings/_rels/drawing1.xml.rels", relsDibujo());
                for (int i = 0; i < imagenes.size(); i++) {
                    zip.putNextEntry(new ZipEntry("xl/media/image" + (i + 1) + ".png"));
                    zip.write(imagenes.get(i).png());
                    zip.closeEntry();
                }
            }
            zip.finish();
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Error al generar el Excel", e);
        }
    }

    private void poner(int fila, int col, Celda c) {
        celdas.computeIfAbsent(fila, k -> new TreeMap<>()).put(col, c);
        ultimaCol = Math.max(ultimaCol, col);
    }

    private static void entrada(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    private String contentTypes() {
        StringBuilder sb = new StringBuilder(XML)
                .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Default Extension=\"png\" ContentType=\"image/png\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                .append("<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
                .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        if (!imagenes.isEmpty()) {
            sb.append("<Override PartName=\"/xl/drawings/drawing1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.drawing+xml\"/>");
        }
        return sb.append("</Types>").toString();
    }

    private String relsRaiz() {
        return XML + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private String workbook() {
        return XML + "<workbook xmlns=\"" + NS + "\" xmlns:r=\"" + NS_R + "\">"
                + "<bookViews><workbookView/></bookViews>"
                + "<sheets><sheet name=\"" + esc(nombreHoja) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                + "<calcPr calcId=\"191029\" fullCalcOnLoad=\"1\"/>"
                + "</workbook>";
    }

    private String relsLibro() {
        return XML + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                + "</Relationships>";
    }

    private String relsHoja() {
        return XML + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/drawing\" Target=\"../drawings/drawing1.xml\"/>"
                + "</Relationships>";
    }

    private String relsDibujo() {
        StringBuilder sb = new StringBuilder(XML)
                .append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 0; i < imagenes.size(); i++) {
            sb.append("<Relationship Id=\"rId").append(i + 1)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"../media/image")
                    .append(i + 1).append(".png\"/>");
        }
        return sb.append("</Relationships>").toString();
    }

    private static long emu(double px) {
        return Math.round(px * 9525);
    }

    private String dibujo() {
        StringBuilder sb = new StringBuilder(XML)
                .append("<xdr:wsDr xmlns:xdr=\"http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing\" ")
                .append("xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" xmlns:r=\"").append(NS_R).append("\">");
        for (int i = 0; i < imagenes.size(); i++) {
            Imagen im = imagenes.get(i);
            sb.append("<xdr:oneCellAnchor><xdr:from><xdr:col>").append(im.col()).append("</xdr:col><xdr:colOff>")
                    .append(emu(im.offXpx())).append("</xdr:colOff><xdr:row>").append(im.fila())
                    .append("</xdr:row><xdr:rowOff>").append(emu(im.offYpx())).append("</xdr:rowOff></xdr:from>")
                    .append("<xdr:ext cx=\"").append(emu(im.wpx())).append("\" cy=\"").append(emu(im.hpx())).append("\"/>")
                    .append("<xdr:pic><xdr:nvPicPr><xdr:cNvPr id=\"").append(i + 2).append("\" name=\"Imagen ").append(i + 1)
                    .append("\"/><xdr:cNvPicPr><a:picLocks noChangeAspect=\"1\"/></xdr:cNvPicPr></xdr:nvPicPr>")
                    .append("<xdr:blipFill><a:blip r:embed=\"rId").append(i + 1).append("\"/><a:stretch><a:fillRect/></a:stretch></xdr:blipFill>")
                    .append("<xdr:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"").append(emu(im.wpx())).append("\" cy=\"")
                    .append(emu(im.hpx())).append("\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></xdr:spPr></xdr:pic>")
                    .append("<xdr:clientData/></xdr:oneCellAnchor>");
        }
        return sb.append("</xdr:wsDr>").toString();
    }

    // ------------------------------------------------------------------ hoja

    private String hoja() {
        int ultimaFila = celdas.isEmpty() ? 0 : celdas.lastKey();
        StringBuilder sb = new StringBuilder(XML)
                .append("<worksheet xmlns=\"").append(NS).append("\" xmlns:r=\"").append(NS_R).append("\">")
                .append("<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>")
                .append("<dimension ref=\"A1:").append(ref(ultimaFila, ultimaCol)).append("\"/>")
                .append("<sheetViews><sheetView showGridLines=\"0\" workbookViewId=\"0\"/></sheetViews>")
                .append("<sheetFormatPr defaultRowHeight=\"15\"/>");
        if (!anchos.isEmpty()) {
            sb.append("<cols>");
            for (Map.Entry<Integer, Double> e : anchos.entrySet()) {
                sb.append("<col min=\"").append(e.getKey() + 1).append("\" max=\"").append(e.getKey() + 1)
                        .append("\" width=\"").append(num(e.getValue())).append("\" customWidth=\"1\"/>");
            }
            sb.append("</cols>");
        }
        sb.append("<sheetData>");
        for (int f = 0; f <= ultimaFila; f++) {
            TreeMap<Integer, Celda> fila = celdas.get(f);
            Double alto = altos.get(f);
            if (fila == null && alto == null) {
                continue;
            }
            sb.append("<row r=\"").append(f + 1).append("\"");
            if (alto != null) {
                sb.append(" ht=\"").append(num(alto)).append("\" customHeight=\"1\"");
            }
            sb.append(">");
            if (fila != null) {
                for (Map.Entry<Integer, Celda> e : fila.entrySet()) {
                    sb.append(celdaXml(f, e.getKey(), e.getValue()));
                }
            }
            sb.append("</row>");
        }
        sb.append("</sheetData>");
        if (!combinadas.isEmpty()) {
            sb.append("<mergeCells count=\"").append(combinadas.size()).append("\">");
            for (int[] m : combinadas) {
                sb.append("<mergeCell ref=\"").append(ref(m[0], m[1])).append(":").append(ref(m[2], m[3])).append("\"/>");
            }
            sb.append("</mergeCells>");
        }
        sb.append("<pageMargins left=\"0.4\" right=\"0.4\" top=\"0.5\" bottom=\"0.5\" header=\"0.3\" footer=\"0.3\"/>")
                .append("<pageSetup orientation=\"landscape\" paperSize=\"9\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
        if (!imagenes.isEmpty()) {
            sb.append("<drawing r:id=\"rId1\"/>");
        }
        return sb.append("</worksheet>").toString();
    }

    private String celdaXml(int f, int c, Celda cel) {
        String r = ref(f, c);
        String s = " s=\"" + cel.estilo() + "\"";
        return switch (cel.tipo()) {
            case 's' -> "<c r=\"" + r + "\"" + s + " t=\"inlineStr\"><is><t xml:space=\"preserve\">" + esc(cel.texto()) + "</t></is></c>";
            case 'n' -> "<c r=\"" + r + "\"" + s + "><v>" + num(cel.numero()) + "</v></c>";
            case 'f' -> "<c r=\"" + r + "\"" + s + "><f>" + esc(cel.formula()) + "</f><v>" + num(cel.numero()) + "</v></c>";
            default -> "<c r=\"" + r + "\"" + s + "/>";
        };
    }

    // ------------------------------------------------------------------ estilos

    private String styles() {
        List<String> fuentes = new ArrayList<>();
        List<String> rellenos = new ArrayList<>(List.of(
                "<fill><patternFill patternType=\"none\"/></fill>", "<fill><patternFill patternType=\"gray125\"/></fill>"));
        List<String> bordes = new ArrayList<>(List.of("<border><left/><right/><top/><bottom/><diagonal/></border>"));
        List<String> formatos = new ArrayList<>();
        StringBuilder xfs = new StringBuilder();

        for (Estilo e : estilos.keySet()) {
            String fuente = "<font>" + (e.negrita() ? "<b/>" : "") + "<sz val=\"" + num(e.pt()) + "\"/><color rgb=\"FF" + e.color()
                    + "\"/><name val=\"Calibri\"/><family val=\"2\"/></font>";
            int fontId = indice(fuentes, fuente, 0);
            int fillId = 0;
            if (e.relleno() != null) {
                fillId = indice(rellenos, "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF" + e.relleno()
                        + "\"/><bgColor indexed=\"64\"/></patternFill></fill>", 0);
            }
            int borderId = 0;
            if (e.borde() != null) {
                String lado = " style=\"thin\"><color rgb=\"FF" + e.borde() + "\"/>";
                borderId = indice(bordes, "<border><left" + lado + "</left><right" + lado + "</right><top" + lado
                        + "</top><bottom" + lado + "</bottom><diagonal/></border>", 0);
            }
            int numFmtId = 0;
            if (e.formato() != null) {
                numFmtId = 164 + indice(formatos, e.formato(), 0);
            }
            xfs.append("<xf numFmtId=\"").append(numFmtId).append("\" fontId=\"").append(fontId)
                    .append("\" fillId=\"").append(fillId).append("\" borderId=\"").append(borderId).append("\" xfId=\"0\"")
                    .append(numFmtId != 0 ? " applyNumberFormat=\"1\"" : "").append(" applyFont=\"1\"")
                    .append(fillId != 0 ? " applyFill=\"1\"" : "").append(borderId != 0 ? " applyBorder=\"1\"" : "")
                    .append(" applyAlignment=\"1\"><alignment horizontal=\"")
                    .append(e.horizontal() == null ? "general" : e.horizontal()).append("\" vertical=\"")
                    .append(e.vertical() == null ? "center" : e.vertical()).append("\"")
                    .append(e.ajustar() ? " wrapText=\"1\"" : "").append("/></xf>");
        }

        StringBuilder sb = new StringBuilder(XML).append("<styleSheet xmlns=\"").append(NS).append("\">");
        if (!formatos.isEmpty()) {
            sb.append("<numFmts count=\"").append(formatos.size()).append("\">");
            for (int i = 0; i < formatos.size(); i++) {
                sb.append("<numFmt numFmtId=\"").append(164 + i).append("\" formatCode=\"").append(esc(formatos.get(i))).append("\"/>");
            }
            sb.append("</numFmts>");
        }
        sb.append("<fonts count=\"").append(fuentes.size()).append("\">").append(String.join("", fuentes)).append("</fonts>")
                .append("<fills count=\"").append(rellenos.size()).append("\">").append(String.join("", rellenos)).append("</fills>")
                .append("<borders count=\"").append(bordes.size()).append("\">").append(String.join("", bordes)).append("</borders>")
                .append("<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>")
                .append("<cellXfs count=\"").append(estilos.size()).append("\">").append(xfs).append("</cellXfs>")
                .append("<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>")
                .append("</styleSheet>");
        return sb.toString();
    }

    private static int indice(List<String> lista, String valor, int base) {
        int i = lista.indexOf(valor);
        if (i < 0) {
            lista.add(valor);
            i = lista.size() - 1;
        }
        return i + base;
    }

    // ------------------------------------------------------------------ utilidades

    static String ref(int fila, int col) {
        StringBuilder letras = new StringBuilder();
        int c = col;
        do {
            letras.insert(0, (char) ('A' + c % 26));
            c = c / 26 - 1;
        } while (c >= 0);
        return letras.toString() + (fila + 1);
    }

    private static String num(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) {
            return String.valueOf((long) v);
        }
        return String.format(Locale.ROOT, "%.6f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> {
                    if (ch >= 0x20 || ch == '\n' || ch == '\t' || ch == '\r') {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }
}
