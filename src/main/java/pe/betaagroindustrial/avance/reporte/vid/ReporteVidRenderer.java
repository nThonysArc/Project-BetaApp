package pe.betaagroindustrial.avance.reporte.vid;

import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.BloqueHora;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.FilaModulo;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.ProcesadoDia;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.Resultado;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.ResumenSupervisor;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dibuja el "Avance por hora de seleccion" de Vid como PNG, reproduciendo la plantilla de Excel
 * que hoy se llena a mano (guinda, filas por modulo, bloques por hora, cuadros de PROCESADO,
 * resumen por supervisor).
 *
 * Solo DIBUJA: todos los numeros llegan calculados en {@link Resultado}. Las coordenadas estan
 * en "unidades de plantilla" (1 unidad = 2 px de la captura original de 1680 px de ancho), asi
 * que a escala 2.0 la imagen sale del mismo tamano que la plantilla. Se mide primero y se pinta
 * despues, de modo que la imagen crece sola con cada hora. Las horas se muestran en
 * formato de 24 h ("14:15"), como en el avance que hoy arma el equipo.
 */
public final class ReporteVidRenderer {

    /** logo y fotos son opcionales (null = se omiten). */
    public record Entrada(
            String titulo,
            LocalDate fechaProceso,
            Resultado resultado,
            byte[] logo,
            byte[] fotoIzquierda,
            byte[] fotoDerecha
    ) {
    }

    // ---- Paleta de la plantilla ------------------------------------------------------------
    private static final Color GUINDA = new Color(184, 17, 52);
    private static final Color GUINDA_OSCURO = new Color(172, 16, 49);
    private static final Color GUINDA_LINEA = new Color(120, 10, 35);
    private static final Color ROSA = new Color(247, 233, 236);
    private static final Color ROSA_FUERTE = new Color(236, 200, 208);
    private static final Color ROSA_BORDE = new Color(214, 170, 180);
    private static final Color NEGRO = Color.BLACK;
    private static final Color BLANCO = Color.WHITE;

    // ---- Geometria (unidades de plantilla) -------------------------------------------------
    /** Borde derecho de la tabla (las columnas E se miden desde la izquierda del lienzo original). */
    private static final double ANCHO = 840;
    /** La plantilla deja margen a ambos lados: se corre el cuerpo a la izquierda y el lienzo es mas angosto. */
    private static final double DESPLAZAMIENTO = -60;
    private static final double LIENZO = 822;
    /** Una hora con pocos modulos no se achica por debajo de esto (como las filas altas de la plantilla). */
    private static final double MIN_BLOQUE = 50;
    private static final float FUENTE_DATO = 7.4f;
    private static final float FUENTE_OBS = 7.1f;
    /** Bordes de columna de la tabla: HORA | MODULO | PERSONAS | JABAS/HORA | TOTAL | ACUM | REND | OBS. */
    private static final double[] E = {131, 179.5, 249.5, 339, 412.5, 491.5, 571.5, 641, 840};
    private static final int C_HORA = 0, C_MODULO = 1, C_PERSONAS = 2, C_JABAS = 3, C_TOTAL = 4,
            C_ACUM = 5, C_REND = 6, C_OBS = 7;

    private static final double ALTO_ENCABEZADO = 30;
    private static final double ALTO_FILA = 33;
    private static final double LINEA_OBS = 9.2;

    private static final Font REGULAR = cargar("DejaVuSans.ttf", Font.PLAIN);
    private static final Font NEGRITA = cargar("DejaVuSans-Bold.ttf", Font.BOLD);

    private ReporteVidRenderer() {
    }

    public static byte[] renderizarPng(Entrada e, double escala) {
        BufferedImage medida = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D gm = medida.createGraphics();
        configurar(gm);
        double alto = pintar(gm, e);
        gm.dispose();

        int pw = (int) Math.round(LIENZO * escala);
        int ph = (int) Math.ceil(alto * escala);
        BufferedImage img = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        configurar(g);
        g.setColor(BLANCO);
        g.fillRect(0, 0, pw, ph);
        g.scale(escala, escala);
        pintar(g, e);
        g.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Error al generar la imagen del reporte VID", ex);
        }
    }

    // ======================================================================
    // Dibujo (devuelve el alto total usado)
    // ======================================================================

    private static double pintar(Graphics2D g, Entrada e) {
        Resultado r = e.resultado();

        cabecera(g, e);
        AffineTransform original = g.getTransform();
        g.translate(DESPLAZAMIENTO, 0);
        double finIndicadores = indicadores(g, r);
        double yTabla = Math.max(166, finIndicadores + 28);
        double finTabla = tabla(g, r, yTabla);
        double finPie = pie(g, r, finTabla);
        double finResumen = resumenSupervisores(g, r, finTabla + 106);
        g.setTransform(original);
        return Math.max(finResumen, finPie) + 24;
    }

    /** Se dibuja en coordenadas finales del lienzo (antes de correr el cuerpo). */
    private static void cabecera(Graphics2D g, Entrada e) {
        imagen(g, e.logo(), 26, 8, 102, 48);
        imagen(g, e.fotoIzquierda(), 140, 8, 60, 52);
        imagen(g, e.fotoDerecha(), 668, 6, 62, 52);
        texto(g, e.titulo(), 0, 22, 862, 30, 1, true, 15.1f, NEGRO, 0);

        texto(g, "FECHA PROCESO:", 32, 76, 100, 15, 0, true, 8.5f, NEGRO, 0);
        caja(g, 122, 77, 72, 13.5, BLANCO, NEGRO, 0.8);
        texto(g, fechaLarga(e.fechaProceso()), 122, 77, 72, 13.5, 1, true, 8.3f, NEGRO, 0);
    }

    /**
     * Hay arrastre cuando lo procesado proviene de mas de una fecha de cosecha (fruta de dias previos):
     * entonces se desglosa por dia, como en la plantilla con stock. Si todo es del mismo dia se muestra
     * "INGRESO HASTA EL MOMENTO", como en la plantilla de proceso en el mismo dia.
     */
    static boolean conArrastre(Resultado r) {
        return r.porDia().size() > 1;
    }

    /** Cabecera de indicadores; su forma depende de {@link #conArrastre}. */
    private static double indicadores(Graphics2D g, Resultado r) {
        double y = 77;
        y = renglonIndicador(g, y, "PROYECCION", plano(r.proyeccion()));
        if (conArrastre(r)) {
            List<ProcesadoDia> dias = new ArrayList<>(r.porDia());
            dias.sort((a, b) -> b.fecha().compareTo(a.fecha()));
            for (ProcesadoDia d : dias) {
                y = renglonIndicador(g, y, fechaCorta(d.fecha()), plano(d.jabas()));
            }
        } else {
            BigDecimal ingreso = r.stock().add(r.totalAcumulado());
            y = renglonIndicador(g, y, "INGRESO HASTA EL MOMENTO", plano(ingreso));
        }
        y = renglonIndicador(g, y + 5, "PROCESADO", num(r.procesadoDia()));
        y = renglonIndicador(g, y + 5, "STOCK TOTAL", plano(r.stock()));
        return y;
    }

    private static double renglonIndicador(Graphics2D g, double y, String etiqueta, String valor) {
        double h = 13.5;
        caja(g, 491.5, y, 149.5, h, GUINDA, null, 0);
        texto(g, etiqueta, 491.5, y, 149.5, h, 1, true, 7.9f, BLANCO, 0);
        caja(g, 641, y, 90.5, h, BLANCO, NEGRO, 0.6);
        texto(g, valor, 641, y, 90.5, h, 1, true, 7.7f, NEGRO, 0);
        caja(g, 731.5, y, 108.5, h, BLANCO, NEGRO, 0.6);
        texto(g, "JABAS", 731.5, y, 108.5, h, 1, false, 7.7f, NEGRO, 0);
        return y + h;
    }

    private static double tabla(Graphics2D g, Resultado r, double y0) {
        double x0 = E[0];

        // Encabezado guinda
        caja(g, x0, y0, ANCHO - x0, ALTO_ENCABEZADO, GUINDA, null, 0);
        String[] enc = {"HORA", "MODULO", "PERSONAS", "JABAS / HORA", "TOTAL JABAS\nPOR HORA", "ACUMULADO", "REND/\nPERSONA", "OBSERVACIONES"};
        for (int c = 0; c < enc.length; c++) {
            if (c == C_OBS) {
                texto(g, enc[c], E[c] + 9, y0, E[c + 1] - E[c] - 9, ALTO_ENCABEZADO, 0, true, 7.65f, BLANCO, 11);
            } else {
                texto(g, enc[c], E[c], y0, E[c + 1] - E[c], ALTO_ENCABEZADO, 1, true, 7.65f, BLANCO, 11);
            }
            if (c > 0) {
                linea(g, E[c], y0, E[c], y0 + ALTO_ENCABEZADO, GUINDA_LINEA, 0.5, false);
            }
        }

        double y = y0 + ALTO_ENCABEZADO;
        for (BloqueHora b : r.bloques()) {
            y = bloque(g, b, y);
        }

        // Marco negro grueso: lado izquierdo y borde superior/inferior de la tabla
        linea(g, x0, y0, x0, y, NEGRO, 1.5, false);
        return y;
    }

    private static double bloque(Graphics2D g, BloqueHora b, double y) {
        List<FilaModulo> filas = b.filas();
        double[] alturas = new double[Math.max(1, filas.size())];
        double total = 0;
        for (int i = 0; i < alturas.length; i++) {
            int lineas = i < filas.size() ? lineasObs(g, filas.get(i)) : 1;
            alturas[i] = lineas <= 1 ? ALTO_FILA : ALTO_FILA + (lineas - 1) * LINEA_OBS * 1.1;
            total += alturas[i];
        }

        if (total < MIN_BLOQUE) {
            double k = MIN_BLOQUE / total;
            for (int i = 0; i < alturas.length; i++) {
                alturas[i] *= k;
            }
            total = MIN_BLOQUE;
        }

        caja(g, E[0], y, ANCHO - E[0], total, ROSA, null, 0);
        for (int c = 1; c < E.length - 1; c++) {
            linea(g, E[c], y, E[c], y + total, BLANCO, 0.6, false);
        }

        // Numero de hora, fuera de la tabla, a la altura de la primera fila
        texto(g, String.valueOf(b.numero()), 0, y, 2 * 106, alturas[0], 1, true, 8.6f, NEGRO, 0);
        // Rango horario, centrado en el bloque
        texto(g, rangoHora(b.inicio(), b.fin()), E[C_HORA], y, E[C_MODULO] - E[C_HORA], total, 1, true, 7.2f, NEGRO, 12);
        // Total y acumulado de la hora (celdas combinadas)
        texto(g, num(b.totalHora()), E[C_TOTAL], y, E[C_ACUM] - E[C_TOTAL], total, 1, true, FUENTE_DATO, NEGRO, 0);
        texto(g, num(b.acumulado()), E[C_ACUM], y, E[C_REND] - E[C_ACUM], total, 1, false, FUENTE_DATO, NEGRO, 0);

        double yy = y;
        for (int i = 0; i < filas.size(); i++) {
            FilaModulo f = filas.get(i);
            double h = alturas[i];
            texto(g, f.modulo(), E[C_MODULO], yy, E[C_PERSONAS] - E[C_MODULO], h, 1, false, FUENTE_DATO, NEGRO, 0);
            texto(g, String.valueOf(f.personas()), E[C_PERSONAS], yy, E[C_JABAS] - E[C_PERSONAS], h, 1, false, FUENTE_DATO, NEGRO, 0);
            texto(g, plano(f.jabas()), E[C_JABAS], yy, E[C_TOTAL] - E[C_JABAS], h, 1, false, FUENTE_DATO, NEGRO, 0);
            texto(g, f.rendimiento() == null ? "0" : f.rendimiento().toPlainString(),
                    E[C_REND], yy, E[C_OBS] - E[C_REND], h, 1, false, FUENTE_DATO, NEGRO, 0);
            texto(g, textoObs(f), E[C_OBS] + 4, yy, E[C_OBS + 1] - E[C_OBS] - 8, h, 1, false, FUENTE_OBS, NEGRO, LINEA_OBS);

            if (i < filas.size() - 1) {
                double yb = yy + h;
                // MODULO..JABAS: punteado; REND y OBSERVACIONES: linea fina continua
                linea(g, E[C_MODULO], yb, E[C_TOTAL], yb, NEGRO, 0.5, true);
                linea(g, E[C_REND], yb, ANCHO, yb, NEGRO, 0.5, false);
            }
            yy += h;
        }

        // Separador grueso entre horas
        linea(g, E[0], y + total, ANCHO, y + total, NEGRO, 1.5, false);
        return y + total;
    }

    /** TOTAL / ACUMULADO ANTERIOR / TOTAL ACUMULADO / STOCK, y el cuadro PROCESADO por dia. */
    private static double pie(Graphics2D g, Resultado r, double yTabla) {
        // Como en la plantilla: TOTAL = ultima hora; ACUMULADO ANTERIOR = lo acumulado hasta la hora previa;
        // TOTAL ACUMULADO = procesado del dia.
        List<BloqueHora> bq = r.bloques();
        BigDecimal ultimaHora = bq.isEmpty() ? BigDecimal.ZERO : bq.get(bq.size() - 1).totalHora();
        BigDecimal acumuladoPrevio = bq.size() < 2 ? BigDecimal.ZERO : bq.get(bq.size() - 2).acumulado();
        String[][] filas = {
                {"TOTAL", num(ultimaHora)},
                {"ACUMULADO ANTERIOR", num(acumuladoPrevio)},
                {"TOTAL ACUMULADO", num(r.procesadoDia())},
                {"STOCK", num(r.stock())}
        };
        double y = yTabla;
        for (String[] f : filas) {
            caja(g, E[C_MODULO], y, E[C_TOTAL] - E[C_MODULO], 13, GUINDA_OSCURO, null, 0);
            texto(g, f[0], E[C_MODULO], y, E[C_JABAS] - E[C_MODULO], 13, 1, true, 7.8f, BLANCO, 0);
            texto(g, f[1], E[C_JABAS], y, E[C_TOTAL] - E[C_JABAS], 13, 1, true, 7.8f, BLANCO, 0);
            linea(g, E[C_MODULO], y, E[C_TOTAL], y, GUINDA_LINEA, 0.5, false);
            y += 13;
        }
        linea(g, E[C_MODULO], y, E[C_TOTAL], y, GUINDA_LINEA, 0.5, false);
        linea(g, E[C_JABAS], yTabla, E[C_JABAS], y, GUINDA_LINEA, 0.5, false);

        if (!conArrastre(r)) {
            return y;
        }

        // Cuadro PROCESADO por dia (27.12 / 28.12 / total)
        double x = E[C_REND];
        double yBarra = yTabla + 73.5;
        caja(g, x, yBarra, 160, 12.5, GUINDA, null, 0);
        texto(g, "PROCESADO", x + 8, yBarra, 150, 12.5, 0, true, 7.9f, BLANCO, 0);

        double yy = yTabla + 106;
        for (ProcesadoDia d : r.porDia()) {
            caja(g, x, yy, 69.5, 24.5, GUINDA, null, 0);
            texto(g, "PROCESADO\n" + fechaPunto(d.fecha()), x, yy, 69.5, 24.5, 1, true, 6.2f, BLANCO, 11.5);
            caja(g, E[C_OBS - 0] - 0, yy, 90.5, 24.5, BLANCO, NEGRO, 0.5);
            texto(g, num(d.jabas()), E[C_OBS] + 2, yy + 5, 54, 18, 0, false, 7.7f, NEGRO, 0);
            texto(g, "JABAS", E[C_OBS] + 36, yy + 5, 52, 18, 2, false, 7.7f, NEGRO, 0);
            yy += 24.5;
        }
        caja(g, E[C_OBS], yy, 90.5, 21, GUINDA, null, 0);
        texto(g, num(r.procesadoDia()), E[C_OBS] + 2, yy, 54, 21, 0, true, 7.9f, BLANCO, 0);
        texto(g, "JABAS", E[C_OBS] + 36, yy, 52, 21, 2, false, 7.9f, BLANCO, 0);
        return Math.max(y, yy + 21);
    }

    /** Un renglon por supervisor + modulo: nombre, modulo, variedad, jabas. Incluye TODOS los modulos. */
    private static double resumenSupervisores(Graphics2D g, Resultado r, double y0) {
        double h = 22;
        double y = y0;
        for (ResumenSupervisor s : r.supervisores()) {
            caja(g, 80, y, 99.5, h, ROSA, null, 0);
            texto(g, s.supervisor(), 80, y, 99.5, h, 1, true, 6.6f, NEGRO, 0);
            caja(g, E[C_MODULO], y, 70, h, ROSA, null, 0);
            texto(g, s.modulo(), E[C_MODULO], y, 70, h, 1, false, FUENTE_DATO, NEGRO, 0);
            caja(g, E[C_PERSONAS], y, 89.5, h, ROSA_FUERTE, null, 0);
            texto(g, mayus(s.variedades()), E[C_PERSONAS], y, 89.5, h, 1, true, 6.6f, NEGRO, 0);
            caja(g, E[C_JABAS], y, 73.5, h, ROSA, null, 0);
            texto(g, plano(s.jabas()), E[C_JABAS], y, 73.5, h, 1, false, FUENTE_DATO, NEGRO, 0);
            caja(g, E[C_TOTAL], y, 79, h, ROSA, null, 0);
            texto(g, "JABAS", E[C_TOTAL], y, 79, h, 1, true, 6.6f, NEGRO, 0);
            // contorno grueso del modulo, como en la plantilla
            caja(g, E[C_MODULO], y, 70, h, null, NEGRO, 1.0);
            y += h;
        }

        double yt = y + 24;
        caja(g, E[C_PERSONAS], yt, E[C_ACUM] - E[C_PERSONAS], 12, ROSA, ROSA_BORDE, 0.6);
        texto(g, "TOTAL", E[C_PERSONAS], yt, 89.5, 12, 1, true, 6.6f, NEGRO, 0);
        texto(g, plano(r.totalSupervisores()), E[C_JABAS], yt, 73.5, 12, 1, false, 6.8f, NEGRO, 0);
        texto(g, "JABAS", E[C_TOTAL], yt, 79, 12, 1, true, 6.6f, NEGRO, 0);
        return yt + 12;
    }

    // ======================================================================
    // Utilidades de dibujo
    // ======================================================================

    private static String textoObs(FilaModulo f) {
        if (f.observacion() != null && !f.observacion().isBlank()) {
            return f.observacion().strip();
        }
        return f.almuerzo() ? "ALMUERZO" : "";
    }

    private static int lineasObs(Graphics2D g, FilaModulo f) {
        return envolver(g, textoObs(f), REGULAR.deriveFont(FUENTE_OBS), E[C_OBS + 1] - E[C_OBS] - 8).size();
    }

    /** Parte el texto en lineas que quepan en {@code ancho}; respeta saltos de linea explicitos. */
    private static List<String> envolver(Graphics2D g, String texto, Font f, double ancho) {
        List<String> out = new ArrayList<>();
        if (texto == null || texto.isEmpty()) {
            out.add("");
            return out;
        }
        FontMetrics fm = g.getFontMetrics(f);
        for (String parrafo : texto.split("\n", -1)) {
            StringBuilder actual = new StringBuilder();
            for (String palabra : parrafo.split("\\s+")) {
                String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
                if (fm.stringWidth(prueba) <= ancho || actual.length() == 0) {
                    actual = new StringBuilder(prueba);
                } else {
                    out.add(actual.toString());
                    actual = new StringBuilder(palabra);
                }
            }
            out.add(actual.toString());
        }
        return out;
    }

    /**
     * Texto en una celda. align: 0 izquierda, 1 centro, 2 derecha; centrado vertical.
     * interlineado: separacion entre lineas (0 = la natural de la fuente).
     */
    private static void texto(Graphics2D g, String s, double x, double y, double w, double h,
                              int align, boolean negrita, float tam, Color color, double interlineado) {
        if (s == null || s.isEmpty()) {
            return;
        }
        Font f = (negrita ? NEGRITA : REGULAR).deriveFont(tam);
        FontMetrics fm = g.getFontMetrics(f);
        List<String> lineas = envolver(g, s, f, w);
        double paso = interlineado > 0 ? interlineado : fm.getHeight();
        double alto = (lineas.size() - 1) * paso + fm.getAscent() * 0.72;
        double base = y + (h - alto) / 2 + fm.getAscent() * 0.72;
        g.setFont(f);
        g.setColor(color);
        for (String l : lineas) {
            double px = switch (align) {
                case 1 -> x + (w - fm.stringWidth(l)) / 2;
                case 2 -> x + w - fm.stringWidth(l);
                default -> x;
            };
            g.drawString(l, (float) px, (float) base);
            base += paso;
        }
    }

    private static void caja(Graphics2D g, double x, double y, double w, double h, Color fondo, Color borde, double grosor) {
        if (fondo != null) {
            g.setColor(fondo);
            g.fill(new java.awt.geom.Rectangle2D.Double(x, y, w, h));
        }
        if (borde != null) {
            g.setColor(borde);
            g.setStroke(new BasicStroke((float) grosor));
            g.draw(new java.awt.geom.Rectangle2D.Double(x, y, w, h));
        }
    }

    private static void linea(Graphics2D g, double x1, double y1, double x2, double y2, Color c, double grosor, boolean punteada) {
        if (grosor <= 0) {
            return;
        }
        g.setColor(c);
        g.setStroke(punteada
                ? new BasicStroke((float) grosor, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{1f, 1f}, 0f)
                : new BasicStroke((float) grosor));
        g.draw(new java.awt.geom.Line2D.Double(x1, y1, x2, y2));
    }

    /** Dibuja la imagen ajustada dentro de la caja, sin deformarla. */
    private static void imagen(Graphics2D g, byte[] bytes, double x, double y, double w, double h) {
        if (bytes == null) {
            return;
        }
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                return;
            }
            double k = Math.min(w / img.getWidth(), h / img.getHeight());
            double iw = img.getWidth() * k;
            double ih = img.getHeight() * k;
            g.drawImage(img, (int) Math.round(x + (w - iw) / 2), (int) Math.round(y + (h - ih) / 2),
                    (int) Math.round(iw), (int) Math.round(ih), null);
        } catch (IOException ignorada) {
            // las imagenes son opcionales
        }
    }

    private static void configurar(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static Font cargar(String archivo, int estilo) {
        try (InputStream in = ReporteVidRenderer.class.getResourceAsStream("/reporte/fonts/" + archivo)) {
            if (in != null) {
                return Font.createFont(Font.TRUETYPE_FONT, in);
            }
        } catch (Exception ignorada) {
            // se usa la fuente del sistema
        }
        return new Font(Font.SANS_SERIF, estilo, 12);
    }

    // ======================================================================
    // Formatos
    // ======================================================================

    /** "4,014": con separador de miles (totales, acumulados, PROCESADO). */
    static String num(BigDecimal v) {
        DecimalFormat df = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(v == null ? BigDecimal.ZERO : v);
    }

    /** "1121": sin separador (jabas por modulo, proyeccion, resumen por supervisor), como en la plantilla. */
    static String plano(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    /** "08:50 AM\nA\n09:50 AM": siempre 12 h con AM/PM correcto (13:00 es "01:00 PM"). */
    static String rangoHora(LocalTime ini, LocalTime fin) {
        return hora24(ini) + "\nA\n" + hora24(fin);
    }

    static String hora24(LocalTime t) {
        return t == null ? "" : String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    static String fechaLarga(LocalDate f) {
        return f == null ? "" : String.format("%02d/%02d/%d", f.getDayOfMonth(), f.getMonthValue(), f.getYear());
    }

    private static final String[] MESES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    /** "28-Dic" */
    static String fechaCorta(LocalDate f) {
        return f == null ? "" : String.format("%02d-%s", f.getDayOfMonth(), MESES[f.getMonthValue() - 1]);
    }

    /** "28.12" */
    static String fechaPunto(LocalDate f) {
        return f == null ? "" : String.format("%02d.%02d", f.getDayOfMonth(), f.getMonthValue());
    }

    private static String mayus(String s) {
        return s == null ? "" : s.toUpperCase(Locale.forLanguageTag("es-PE"));
    }
}
