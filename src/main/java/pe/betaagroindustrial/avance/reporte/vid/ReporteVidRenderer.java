package pe.betaagroindustrial.avance.reporte.vid;

import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.BloqueHora;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.FilaModulo;
import pe.betaagroindustrial.avance.reporte.vid.AvanceVid.Resultado;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Dibuja el "Avance por hora de seleccion" de Vid como PNG.
 *
 * Solo DIBUJA: todos los numeros llegan calculados en {@link Resultado}. Se mide primero
 * (sobre una imagen de 1x1) y se pinta despues, asi la imagen crece con cada hora.
 * Las horas se muestran siempre en formato de 12 h con AM/PM correcto (nunca "13:00 PM").
 */
public final class ReporteVidRenderer {

    public record Entrada(
            String titulo,
            LocalDate fechaProceso,
            Resultado resultado,
            List<Advertencia> advertencias,
            byte[] logo
    ) {
    }

    private static final double ANCHO = 900;
    private static final double M = 10;
    private static final double[] COL = {128, 80, 62, 72, 84, 78, 68, 308}; // suma 880
    private static final String[] ENCABEZADO = {"HORA", "MÓDULO", "PERSONAS", "JABAS / HORA",
            "TOTAL JABAS POR HORA", "ACUMULADO", "REND / PERSONA", "OBSERVACIONES"};

    private static final Color VERDE = new Color(31, 94, 52);
    private static final Color VERDE_CLARO = new Color(226, 240, 217);
    private static final Color GRIS = new Color(242, 242, 242);
    private static final Color AMBAR = new Color(255, 232, 163);
    private static final Color LINEA = new Color(70, 70, 70);
    private static final Color LINEA_SUAVE = new Color(190, 190, 190);
    private static final Color TEXTO = new Color(20, 20, 20);
    private static final Color ROJO = new Color(170, 30, 30);

    private static final Font REGULAR = cargar("Carlito-Regular.ttf", Font.PLAIN);
    private static final Font NEGRITA = cargar("Carlito-Bold.ttf", Font.BOLD);

    private static final double ALTO_FILA = 20;
    private static final double LINEA_OBS = 11.5;
    private static final String[] MESES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    private ReporteVidRenderer() {
    }

    public static byte[] renderizarPng(Entrada e, double escala) {
        BufferedImage medida = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D gm = medida.createGraphics();
        configurar(gm);
        double alto = pintar(gm, e);
        gm.dispose();

        int pw = (int) Math.round(ANCHO * escala);
        int ph = (int) Math.ceil(alto * escala);
        BufferedImage img = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        configurar(g);
        g.setColor(Color.WHITE);
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
        double y = M;

        y = titulo(g, e, y);
        y = indicadores(g, r, y) + 8;
        y = tabla(g, e, y) + 8;
        y = pie(g, r, y) + 8;
        y = supervisores(g, r, y) + 8;
        y = alertas(g, e.advertencias(), y);
        return y + M;
    }

    private static double titulo(Graphics2D g, Entrada e, double y) {
        double h = 44;
        rect(g, M, y, ANCHO - 2 * M, h, VERDE, null);
        double x = M + 8;
        if (e.logo() != null) {
            try {
                BufferedImage logo = ImageIO.read(new ByteArrayInputStream(e.logo()));
                if (logo != null) {
                    double alto = 32;
                    double ancho = alto * logo.getWidth() / logo.getHeight();
                    g.drawImage(logo, (int) x, (int) (y + 6), (int) ancho, (int) alto, null);
                    x += ancho + 10;
                }
            } catch (IOException ignorada) {
                // el logo es opcional
            }
        }
        g.setColor(Color.WHITE);
        g.setFont(NEGRITA.deriveFont(17f));
        g.drawString(e.titulo(), (float) x, (float) (y + 20));
        g.setFont(REGULAR.deriveFont(12f));
        g.drawString("FECHA DE PROCESO: " + fechaLarga(e.fechaProceso()), (float) x, (float) (y + 36));
        return y + h;
    }

    /** PROYECCIÓN | un cuadro por dia de cosecha | PROCESADO | STOCK TOTAL. */
    private static double indicadores(Graphics2D g, Resultado r, double y) {
        List<String[]> cajas = new ArrayList<>();
        cajas.add(new String[]{"PROYECCIÓN", num(r.proyeccion())});
        for (AvanceVid.ProcesadoDia d : r.porDia()) {
            cajas.add(new String[]{fechaCorta(d.fecha()), num(d.jabas())});
        }
        cajas.add(new String[]{"PROCESADO", num(r.procesadoDia())});
        cajas.add(new String[]{"STOCK TOTAL", num(r.stock())});

        double ancho = (ANCHO - 2 * M) / cajas.size();
        double h = 40;
        for (int i = 0; i < cajas.size(); i++) {
            double x = M + i * ancho;
            rect(g, x, y, ancho, h, VERDE_CLARO, LINEA);
            texto(g, cajas.get(i)[0], x, y + 2, ancho, 16, 1, true, 10.5f, VERDE);
            texto(g, cajas.get(i)[1], x, y + 17, ancho, 22, 1, true, 15f, TEXTO);
        }
        return y + h;
    }

    private static double tabla(Graphics2D g, Entrada e, double y) {
        Resultado r = e.resultado();
        double[] xs = columnas();

        // Encabezado
        double hEnc = 30;
        for (int c = 0; c < COL.length; c++) {
            rect(g, xs[c], y, COL[c], hEnc, VERDE, LINEA);
            texto(g, ENCABEZADO[c], xs[c] + 2, y, COL[c] - 4, hEnc, 1, true, 10.5f, Color.WHITE);
        }
        y += hEnc;

        Set<String> marcadas = new HashSet<>();
        for (Advertencia a : e.advertencias()) {
            if (a.hora() != null && a.modulo() != null) {
                marcadas.add(a.hora() + "|" + a.modulo());
            }
        }
        Set<Integer> horasMarcadas = new HashSet<>();
        for (Advertencia a : e.advertencias()) {
            if (a.hora() != null && a.modulo() == null) {
                horasMarcadas.add(a.hora());
            }
        }

        boolean alterna = false;
        for (BloqueHora b : r.bloques()) {
            alterna = !alterna;
            Color fondo = alterna ? Color.WHITE : GRIS;

            double[] alturas = new double[b.filas().size()];
            double total = 0;
            for (int i = 0; i < alturas.length; i++) {
                String obs = textoObs(b.filas().get(i));
                int lineas = envolver(g, obs, REGULAR.deriveFont(9.5f), COL[7] - 8).size();
                alturas[i] = Math.max(ALTO_FILA, lineas * LINEA_OBS + 8);
                total += alturas[i];
            }
            if (alturas.length == 0) {
                total = ALTO_FILA;
            }

            // Celdas combinadas del bloque: HORA, TOTAL y ACUMULADO
            Color fondoHora = horasMarcadas.contains(b.numero()) ? AMBAR : fondo;
            rect(g, xs[0], y, COL[0], total, fondoHora, LINEA);
            texto(g, rangoHora(b.inicio(), b.fin()), xs[0] + 2, y, COL[0] - 4, total, 1, true, 10.5f, TEXTO);
            rect(g, xs[4], y, COL[4], total, fondo, LINEA);
            texto(g, num(b.totalHora()), xs[4], y, COL[4], total, 1, true, 12.5f, TEXTO);
            rect(g, xs[5], y, COL[5], total, fondo, LINEA);
            texto(g, num(b.acumulado()), xs[5], y, COL[5], total, 1, false, 11.5f, TEXTO);

            double yy = y;
            for (int i = 0; i < alturas.length; i++) {
                FilaModulo f = b.filas().get(i);
                double h = alturas[i];
                Color cj = marcadas.contains(b.numero() + "|" + f.modulo()) ? AMBAR : fondo;
                rect(g, xs[1], yy, COL[1], h, fondo, LINEA_SUAVE);
                texto(g, f.modulo(), xs[1] + 2, yy, COL[1] - 4, h, 1, true, 10.5f, TEXTO);
                rect(g, xs[2], yy, COL[2], h, fondo, LINEA_SUAVE);
                texto(g, String.valueOf(f.personas()), xs[2], yy, COL[2], h, 1, false, 11f, TEXTO);
                rect(g, xs[3], yy, COL[3], h, cj, LINEA_SUAVE);
                texto(g, num(f.jabas()), xs[3], yy, COL[3], h, 1, false, 11f, TEXTO);
                rect(g, xs[6], yy, COL[6], h, fondo, LINEA_SUAVE);
                texto(g, f.rendimiento() == null ? "—" : f.rendimiento().toPlainString(), xs[6], yy, COL[6], h, 1, false, 11f, TEXTO);
                rect(g, xs[7], yy, COL[7], h, fondo, LINEA_SUAVE);
                observacion(g, textoObs(f), xs[7] + 4, yy, COL[7] - 8, h);
                yy += h;
            }
            if (alturas.length == 0) {
                rect(g, xs[1], y, COL[1] + COL[2] + COL[3] + COL[6] + COL[7], total, fondo, LINEA_SUAVE);
            }
            y += total;
        }
        // Linea inferior gruesa
        g.setColor(LINEA);
        g.setStroke(new BasicStroke(1.4f));
        g.drawLine((int) M, (int) y, (int) (ANCHO - M), (int) y);
        return y;
    }

    /** TOTAL / ACUMULADO ANTERIOR / TOTAL ACUMULADO / STOCK, y el cuadro PROCESADO por dia. */
    private static double pie(Graphics2D g, Resultado r, double y) {
        double ancho = 360;
        String[][] filas = {
                {"TOTAL", num(r.procesadoDia())},
                {"ACUMULADO ANTERIOR", num(r.acumuladoAnterior())},
                {"TOTAL ACUMULADO", num(r.totalAcumulado())},
                {"STOCK", num(r.stock())}
        };
        double yy = y;
        for (String[] f : filas) {
            boolean fuerte = f[0].equals("TOTAL ACUMULADO") || f[0].equals("STOCK");
            rect(g, M, yy, ancho * 0.6, ALTO_FILA, fuerte ? VERDE_CLARO : Color.WHITE, LINEA);
            texto(g, f[0], M + 4, yy, ancho * 0.6 - 8, ALTO_FILA, 0, true, 10.5f, TEXTO);
            rect(g, M + ancho * 0.6, yy, ancho * 0.4, ALTO_FILA, fuerte ? VERDE_CLARO : Color.WHITE, LINEA);
            texto(g, f[1], M + ancho * 0.6, yy, ancho * 0.4 - 6, ALTO_FILA, 2, true, 11.5f, TEXTO);
            yy += ALTO_FILA;
        }

        double x2 = ANCHO - M - 300;
        rect(g, x2, y, 300, ALTO_FILA, VERDE, LINEA);
        texto(g, "PROCESADO POR DÍA", x2, y, 300, ALTO_FILA, 1, true, 10.5f, Color.WHITE);
        double y2 = y + ALTO_FILA;
        for (AvanceVid.ProcesadoDia d : r.porDia()) {
            rect(g, x2, y2, 180, ALTO_FILA, Color.WHITE, LINEA);
            texto(g, fechaCorta(d.fecha()), x2 + 4, y2, 172, ALTO_FILA, 0, false, 11f, TEXTO);
            rect(g, x2 + 180, y2, 120, ALTO_FILA, Color.WHITE, LINEA);
            texto(g, num(d.jabas()), x2 + 180, y2, 114, ALTO_FILA, 2, false, 11f, TEXTO);
            y2 += ALTO_FILA;
        }
        rect(g, x2, y2, 180, ALTO_FILA, VERDE_CLARO, LINEA);
        texto(g, "TOTAL", x2 + 4, y2, 172, ALTO_FILA, 0, true, 10.5f, TEXTO);
        rect(g, x2 + 180, y2, 120, ALTO_FILA, VERDE_CLARO, LINEA);
        texto(g, num(r.totalPorDia()), x2 + 180, y2, 114, ALTO_FILA, 2, true, 11.5f, TEXTO);
        y2 += ALTO_FILA;
        return Math.max(yy, y2);
    }

    private static double supervisores(Graphics2D g, Resultado r, double y) {
        double[] w = {300, 110, 280, 190};
        String[] enc = {"SUPERVISOR", "MÓDULO", "VARIEDAD", "JABAS"};
        double x = M;
        for (int i = 0; i < w.length; i++) {
            rect(g, x, y, w[i], ALTO_FILA, VERDE, LINEA);
            texto(g, enc[i], x, y, w[i], ALTO_FILA, 1, true, 10.5f, Color.WHITE);
            x += w[i];
        }
        y += ALTO_FILA;
        for (AvanceVid.ResumenSupervisor s : r.supervisores()) {
            String[] v = {s.supervisor(), s.modulo(), s.variedades(), num(s.jabas())};
            x = M;
            for (int i = 0; i < w.length; i++) {
                rect(g, x, y, w[i], ALTO_FILA, Color.WHITE, LINEA_SUAVE);
                texto(g, v[i], x + 4, y, w[i] - 8, ALTO_FILA, i == 3 ? 2 : 0, false, 10.5f, TEXTO);
                x += w[i];
            }
            y += ALTO_FILA;
        }
        rect(g, M, y, w[0] + w[1] + w[2], ALTO_FILA, VERDE_CLARO, LINEA);
        texto(g, "TOTAL", M, y, w[0] + w[1] + w[2] - 6, ALTO_FILA, 2, true, 10.5f, TEXTO);
        rect(g, M + w[0] + w[1] + w[2], y, w[3], ALTO_FILA, VERDE_CLARO, LINEA);
        texto(g, num(r.totalSupervisores()), M + w[0] + w[1] + w[2], y, w[3] - 6, ALTO_FILA, 2, true, 11.5f, TEXTO);
        return y + ALTO_FILA;
    }

    private static double alertas(Graphics2D g, List<Advertencia> adv, double y) {
        if (adv == null || adv.isEmpty()) {
            return y;
        }
        double ancho = ANCHO - 2 * M;
        Font f = REGULAR.deriveFont(10f);
        List<String> lineas = new ArrayList<>();
        for (Advertencia a : adv) {
            lineas.addAll(envolver(g, "• " + a.mensaje(), f, ancho - 16));
        }
        double h = 20 + lineas.size() * 12.5 + 6;
        rect(g, M, y, ancho, h, new Color(255, 245, 224), ROJO);
        texto(g, "REVISAR ANTES DE COMPARTIR (" + adv.size() + ")", M + 8, y, ancho - 16, 20, 0, true, 10.5f, ROJO);
        g.setFont(f);
        g.setColor(TEXTO);
        double yy = y + 20 + 10;
        for (String l : lineas) {
            g.drawString(l, (float) (M + 8), (float) yy);
            yy += 12.5;
        }
        return y + h;
    }

    // ======================================================================
    // Utilidades de dibujo
    // ======================================================================

    private static double[] columnas() {
        double[] xs = new double[COL.length];
        double x = M;
        for (int i = 0; i < COL.length; i++) {
            xs[i] = x;
            x += COL[i];
        }
        return xs;
    }

    private static String textoObs(FilaModulo f) {
        if (f.observacion() != null && !f.observacion().isBlank()) {
            return f.observacion().strip();
        }
        return f.almuerzo() ? "ALMUERZO" : "";
    }

    private static void observacion(Graphics2D g, String texto, double x, double y, double w, double h) {
        if (texto.isEmpty()) {
            return;
        }
        Font f = REGULAR.deriveFont(9.5f);
        List<String> lineas = envolver(g, texto, f, w);
        double total = lineas.size() * LINEA_OBS;
        double base = y + (h - total) / 2 + LINEA_OBS - 2.5;
        g.setFont(f);
        g.setColor(TEXTO);
        for (String l : lineas) {
            g.drawString(l, (float) x, (float) base);
            base += LINEA_OBS;
        }
    }

    /** Parte el texto en lineas que quepan en {@code ancho} px. */
    private static List<String> envolver(Graphics2D g, String texto, Font f, double ancho) {
        List<String> out = new ArrayList<>();
        if (texto == null || texto.isEmpty()) {
            out.add("");
            return out;
        }
        FontMetrics fm = g.getFontMetrics(f);
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split("\\s+")) {
            String prueba = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (fm.stringWidth(prueba) <= ancho || actual.length() == 0) {
                actual = new StringBuilder(prueba);
            } else {
                out.add(actual.toString());
                actual = new StringBuilder(palabra);
            }
        }
        out.add(actual.toString());
        return out;
    }

    /** align: 0 izquierda, 1 centro, 2 derecha; centrado vertical en la celda. */
    private static void texto(Graphics2D g, String s, double x, double y, double w, double h,
                              int align, boolean negrita, float tam, Color color) {
        if (s == null || s.isEmpty()) {
            return;
        }
        Font f = (negrita ? NEGRITA : REGULAR).deriveFont(tam);
        FontMetrics fm = g.getFontMetrics(f);
        // Cabeceras largas: se parten en 2 lineas.
        List<String> lineas = fm.stringWidth(s) > w ? envolver(g, s, f, w) : List.of(s);
        double alto = lineas.size() * fm.getHeight();
        double base = y + (h - alto) / 2 + fm.getAscent();
        g.setFont(f);
        g.setColor(color);
        for (String l : lineas) {
            double px = switch (align) {
                case 1 -> x + (w - fm.stringWidth(l)) / 2;
                case 2 -> x + w - fm.stringWidth(l);
                default -> x;
            };
            g.drawString(l, (float) px, (float) base);
            base += fm.getHeight();
        }
    }

    private static void rect(Graphics2D g, double x, double y, double w, double h, Color fondo, Color borde) {
        if (fondo != null) {
            g.setColor(fondo);
            g.fillRect((int) Math.round(x), (int) Math.round(y), (int) Math.round(w), (int) Math.round(h));
        }
        if (borde != null) {
            g.setColor(borde);
            g.setStroke(new BasicStroke(0.8f));
            g.drawRect((int) Math.round(x), (int) Math.round(y), (int) Math.round(w), (int) Math.round(h));
        }
    }

    private static void configurar(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
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

    static String num(BigDecimal v) {
        DecimalFormat df = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(v == null ? BigDecimal.ZERO : v);
    }

    /** "08:50 AM a 09:50 AM": siempre 12 h con AM/PM correcto (13:00 es "01:00 PM"). */
    static String rangoHora(LocalTime ini, LocalTime fin) {
        return hora12(ini) + " a " + hora12(fin);
    }

    static String hora12(LocalTime t) {
        if (t == null) {
            return "";
        }
        int h = t.getHour() % 12 == 0 ? 12 : t.getHour() % 12;
        return String.format("%02d:%02d %s", h, t.getMinute(), t.getHour() < 12 ? "AM" : "PM");
    }

    static String fechaLarga(LocalDate f) {
        return f == null ? "" : String.format("%02d/%02d/%d", f.getDayOfMonth(), f.getMonthValue(), f.getYear());
    }

    static String fechaCorta(LocalDate f) {
        return f == null ? "" : String.format("%02d-%s", f.getDayOfMonth(), MESES[f.getMonthValue() - 1]);
    }
}
