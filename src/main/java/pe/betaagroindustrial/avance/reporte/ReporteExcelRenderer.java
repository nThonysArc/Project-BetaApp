package pe.betaagroindustrial.avance.reporte;

import pe.betaagroindustrial.avance.reporte.ReporteDatos.CorteReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.FilaReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.LoteReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.MaquinaCorteReporte;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dibuja el reporte de avance de produccion con el mismo aspecto que la
 * captura de Excel que se envia cada hora: encabezado con logo y foto,
 * "TOTAL A PROCESAR", tabla por hora con celdas combinadas, pie de totales
 * y resumen por maquina con su supervisor.
 *
 * Todas las medidas estan en "pixeles del Excel original" (886 px de ancho)
 * y se escalan por {@code escala} al generar el PNG (2.0 = el doble de nitidez).
 * El reporte CRECE verticalmente: cada corte agrega un bloque de filas, y las
 * filas se agrandan si la observacion de una maquina necesita mas lineas.
 *
 * Usa solo Java2D (sin navegador ni dependencias nuevas). La tipografia es
 * Carlito (equivalente metrico de Calibri, licencia OFL) embebida en
 * /reporte/fonts, para que el resultado sea igual en cualquier servidor. El
 * Excel original esta comprimido horizontalmente (~0.75), por eso el texto se
 * dibuja con esa misma condensacion.
 */
public final class ReporteExcelRenderer {

    // ---------- Colores medidos sobre el Excel original ----------
    private static final Color MORADO = new Color(112, 48, 160);
    private static final Color MORADO_LINEA = new Color(84, 34, 120);
    private static final Color LILA = new Color(251, 233, 249);
    private static final Color LILA_SUBTOTAL = new Color(238, 193, 241);
    private static final Color GRIS_STOCK = new Color(237, 237, 237);
    private static final Color LINEA = new Color(52, 52, 52);
    private static final Color LINEA_SUAVE = new Color(191, 191, 191);
    private static final Color TEXTO = new Color(18, 18, 18);
    private static final Color TEXTO_TENUE = new Color(70, 70, 70);
    private static final Color BLANCO = Color.WHITE;

    // ---------- Geometria (pixeles del Excel original) ----------
    private static final double ANCHO = 886;
    /** Bordes verticales de las 13 columnas de la tabla principal. */
    private static final double[] C = {41, 92, 144, 218, 258, 318, 366, 417, 479, 541, 593, 655, 692, 885};
    private static final int HORA = 0, FECHA = 1, VARIEDAD = 2, JABAS = 3, PESO_VIAJE = 4, PESO_MAQ = 5,
            TOTAL_HORA = 6, ACUM = 7, MAQUINA = 8, KATO = 9, EMPAC = 10, KG_EMP = 11, OBS = 12;
    private static final double ALTO_ENCABEZADO = 21;
    private static final double ALTO_FILA = 20;
    private static final double ALTO_MIN_BLOQUE = 34;
    private static final double ALTO_PAUSA = 38;
    private static final double INTERLINEADO_OBS = 9.6;
    private static final double ALTO_FILA_PIE = 14.2;
    private static final double MIN_Y_TABLA = 146;

    // ---------- Tipografia ----------
    /** Condensacion horizontal del Excel original respecto a la fuente natural. */
    private static final double K = 0.751;
    private static final double ALTURA_MAYUSCULA = 0.644;
    private static final double T_TITULO = 21.3, T_ETIQUETA = 12.2, T_FECHA = 11.2, T_ENCABEZADO = 10.7,
            T_CUERPO = 9.6, T_OBS = 8.0, T_PIE = 10.6, T_MINI = 9.2, T_HORA = 11.0, T_MP = 9.4;

    private static final Font BASE_REGULAR = cargarFuente("Carlito-Regular.ttf", Font.PLAIN);
    private static final Font BASE_NEGRITA = cargarFuente("Carlito-Bold.ttf", Font.BOLD);
    private static final Map<String, Font> CACHE_FUENTES = new HashMap<>();

    private static final int IZQ = 0, CENTRO = 1, DER = 2;

    private static final String[] ENCABEZADOS = {
            "HORA", "FECHA DE\nCOSECHA", "VARIEDAD", "JABAS", "PESO POR VIAJE", "PESO POR\nMAQUINA",
            "TOTAL PESO\nPOR HORA", "ACUM. X HORA", "MAQUINA", "KATO´S", "EMPACADORES", "KG/EMP", "OBSERVACION"
    };
    private static final String[] MESES = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    private ReporteExcelRenderer() {
    }

    // =====================================================================
    // API
    // =====================================================================

    public static byte[] renderizarPng(ReporteDatos datos, double escala) {
        BufferedImage medida = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D gm = medida.createGraphics();
        configurar(gm);
        Diseno diseno = new Diseno(datos, new Pintor(gm));
        gm.dispose();

        int pw = (int) Math.round(ANCHO * escala);
        int ph = (int) Math.ceil(diseno.altoTotal * escala);
        BufferedImage imagen = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        configurar(g);
        g.setColor(BLANCO);
        g.fillRect(0, 0, pw, ph);
        g.scale(escala, escala);
        diseno.dibujar(new Pintor(g));
        g.dispose();

        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Error al generar la imagen del reporte", e);
        }
    }

    // =====================================================================
    // Layout: calcula alturas y posiciones antes de dibujar
    // =====================================================================

    private static final class Grupo {
        Long maquinaId;
        List<FilaReporte> filas = new ArrayList<>();
        String observacion;
        List<String> lineasObs = List.of();
        double alto;
        BigDecimal pesoMaquina = BigDecimal.ZERO;
        boolean almuerzo;
    }

    private static final class Bloque {
        CorteReporte corte;
        int numero;
        /** Barra "ALMUERZO DEL PERSONAL": todas las maquinas pararon, no es una hora de produccion. */
        boolean pausa;
        List<Grupo> grupos = new ArrayList<>();
        List<String> lineasObsGeneral = null;
        double y;
        double alto;
        BigDecimal acumulado = BigDecimal.ZERO;
    }

    private static final class Tramo {
        final String supervisor;
        final LocalTime inicio;
        LocalTime fin;

        Tramo(String supervisor, LocalTime inicio, LocalTime fin) {
            this.supervisor = supervisor;
            this.inicio = inicio;
            this.fin = fin;
        }
    }

    private static final class ResumenMaquina {
        String nombre;
        BigDecimal jabas = BigDecimal.ZERO;
        BigDecimal kilos = BigDecimal.ZERO;
        List<Tramo> tramos = new ArrayList<>();
        List<String> lineas = List.of();
        double alto;
    }

    private static final class Diseno {
        final ReporteDatos d;
        final List<Bloque> bloques = new ArrayList<>();
        final List<ResumenMaquina> resumen = new ArrayList<>();
        BigDecimal mpJabas = BigDecimal.ZERO, mpKilos = BigDecimal.ZERO;
        BigDecimal totJabas = BigDecimal.ZERO, totKilos = BigDecimal.ZERO;
        BigDecimal ultJabas = BigDecimal.ZERO, ultKilos = BigDecimal.ZERO;
        double yMiniFilas, ySubtotal, yMp, yTabla, yPie, yResumen, yTotalResumen, yFinTabla, altoTotal;

        Diseno(ReporteDatos d, Pintor medidor) {
            this.d = d;
            calcularTotales();
            calcularPosicionesSuperiores();
            calcularBloques(medidor);
            calcularResumen(medidor);
        }

        private void calcularTotales() {
            for (LoteReporte l : d.ingresos()) {
                mpJabas = mpJabas.add(nz(l.jabas()));
                mpKilos = mpKilos.add(nz(l.kilos()));
            }
            CorteReporte ultimo = null;
            for (CorteReporte c : d.cortes()) {
                totJabas = totJabas.add(nz(c.jabasTotal()));
                totKilos = totKilos.add(nz(c.pesoTotal()));
                if (!c.todoAlmuerzo()) {
                    ultimo = c;
                }
            }
            // "TOTAL" del pie = ultima hora de produccion (la barra de almuerzo no cuenta)
            if (ultimo != null) {
                ultJabas = nz(ultimo.jabasTotal());
                ultKilos = nz(ultimo.pesoTotal());
            }
        }

        private void calcularPosicionesSuperiores() {
            int n = d.ingresos().size();
            yMiniFilas = 43 + 11 + 0.5;
            ySubtotal = yMiniFilas + 10.1 * n;
            yMp = ySubtotal + 10.5 + 7;
            double finStock = yMp + 26 + 10.5;
            yTabla = Math.max(MIN_Y_TABLA, finStock + 7.2);
        }

        private void calcularBloques(Pintor p) {
            Font fObs = fuente(false, T_OBS);
            double anchoObs = C[13] - C[OBS] - 10;
            double y = yTabla + ALTO_ENCABEZADO;
            BigDecimal acumulado = BigDecimal.ZERO;
            int numero = 0;

            for (CorteReporte corte : d.cortes()) {
                Bloque b = new Bloque();
                b.corte = corte;
                b.y = y;

                if (corte.todoAlmuerzo()) {
                    b.pausa = true;
                    b.alto = ALTO_PAUSA;
                    b.acumulado = acumulado;
                    bloques.add(b);
                    y += b.alto;
                    continue;
                }
                b.numero = ++numero;

                Map<Long, MaquinaCorteReporte> info = new HashMap<>();
                for (MaquinaCorteReporte m : corte.maquinas()) {
                    info.put(m.maquinaId(), m);
                }

                Map<Long, Grupo> porMaquina = new LinkedHashMap<>();
                for (FilaReporte f : corte.filas()) {
                    Grupo g = porMaquina.computeIfAbsent(f.maquinaId(), id -> {
                        Grupo nuevo = new Grupo();
                        nuevo.maquinaId = id;
                        return nuevo;
                    });
                    g.filas.add(f);
                    g.pesoMaquina = g.pesoMaquina.add(nz(f.peso()));
                }

                boolean hayObsMaquina = false;
                for (Grupo g : porMaquina.values()) {
                    MaquinaCorteReporte m = info.get(g.maquinaId);
                    g.observacion = m != null ? m.observacion() : null;
                    g.almuerzo = m != null && m.almuerzo();
                    if (!vacio(g.observacion)) {
                        hayObsMaquina = true;
                    }
                }

                for (Grupo g : porMaquina.values()) {
                    g.lineasObs = vacio(g.observacion) ? List.of() : p.ajustar(mayus(g.observacion), fObs, anchoObs);
                    g.alto = Math.max(g.filas.size() * ALTO_FILA, g.lineasObs.isEmpty() ? 0 : g.lineasObs.size() * INTERLINEADO_OBS + 8);
                    if (g.almuerzo) {
                        g.alto = Math.max(g.alto, 28); // cabe el texto "ALMUERZO DE PERSONAL / MAQUINA N"
                    }
                    b.grupos.add(g);
                }

                double alto = 0;
                for (Grupo g : b.grupos) {
                    alto += g.alto;
                }

                // Sin observaciones por maquina: la observacion general ocupa toda la hora
                double minimo = ALTO_MIN_BLOQUE;
                if (!hayObsMaquina && !vacio(corte.observacionGeneral())) {
                    b.lineasObsGeneral = p.ajustar(mayus(corte.observacionGeneral()), fObs, anchoObs);
                    minimo = Math.max(minimo, b.lineasObsGeneral.size() * INTERLINEADO_OBS + 8);
                }
                if (alto < minimo) {
                    if (b.grupos.isEmpty()) {
                        alto = minimo;
                    } else {
                        b.grupos.get(b.grupos.size() - 1).alto += minimo - alto;
                        alto = minimo;
                    }
                }

                b.alto = alto;
                acumulado = acumulado.add(nz(corte.pesoTotal()));
                b.acumulado = acumulado;
                bloques.add(b);
                y += alto;
            }

            if (bloques.isEmpty()) {
                y += ALTO_MIN_BLOQUE;
            }
            yFinTabla = y;
            yPie = yFinTabla;
        }

        private void calcularResumen(Pintor p) {
            Font fSup = fuente(true, T_PIE);
            Map<Long, ResumenMaquina> porMaquina = new LinkedHashMap<>();

            for (Bloque b : bloques) {
                Map<Long, MaquinaCorteReporte> info = new HashMap<>();
                for (MaquinaCorteReporte m : b.corte.maquinas()) {
                    info.put(m.maquinaId(), m);
                }
                for (Grupo g : b.grupos) {
                    ResumenMaquina r = porMaquina.computeIfAbsent(g.maquinaId, id -> new ResumenMaquina());
                    r.nombre = mayus(g.filas.get(0).maquina());
                    for (FilaReporte f : g.filas) {
                        r.jabas = r.jabas.add(nz(f.jabas()));
                        r.kilos = r.kilos.add(nz(f.peso()));
                    }
                    MaquinaCorteReporte m = info.get(g.maquinaId);
                    String sup = m != null && !vacio(m.supervisor()) ? mayus(m.supervisor()) : "-";
                    Tramo ultimo = r.tramos.isEmpty() ? null : r.tramos.get(r.tramos.size() - 1);
                    if (ultimo != null && ultimo.supervisor.equals(sup)) {
                        ultimo.fin = b.corte.fin();
                    } else {
                        r.tramos.add(new Tramo(sup, b.corte.inicio(), b.corte.fin()));
                    }
                }
            }

            double anchoSup = C[PESO_VIAJE + 1] - C[JABAS] - 6;
            for (ResumenMaquina r : porMaquina.values()) {
                if (r.tramos.size() == 1) {
                    String sup = r.tramos.get(0).supervisor;
                    r.lineas = "-".equals(sup) ? List.of() : p.ajustar(sup, fSup, anchoSup);
                } else {
                    List<String> lineas = new ArrayList<>();
                    for (Tramo t : r.tramos) {
                        lineas.addAll(p.ajustar(t.supervisor + " (" + hora(t.inicio) + "-" + hora(t.fin) + ")", fSup, anchoSup));
                    }
                    r.lineas = lineas;
                }
                r.alto = Math.max(20, r.lineas.size() * 10.6 + 8);
                resumen.add(r);
            }

            yResumen = yPie + 4 * ALTO_FILA_PIE + 21;
            double fin = yResumen + 20;
            for (ResumenMaquina r : resumen) {
                fin += r.alto;
            }
            yTotalResumen = fin;
            altoTotal = Math.ceil(fin + 19.7 + 7);
        }

        // -----------------------------------------------------------------
        // Dibujo
        // -----------------------------------------------------------------

        void dibujar(Pintor p) {
            dibujarEncabezadoSuperior(p);
            dibujarTablaPrincipal(p);
            dibujarPie(p);
            dibujarResumen(p);
        }

        private void dibujarEncabezadoSuperior(Pintor p) {
            // Logo y foto del producto
            p.imagen(d.logo(), 42, 18, 95, 49);
            p.imagen(d.fotoProducto(), 788, 21, 86, 55);

            // Titulo
            p.texto(mayus(d.titulo()), fuente(true, T_TITULO), TEXTO, 140.5, 17, 620, 21, CENTRO);

            // Fecha del proceso
            Font fEtiqueta = fuente(true, T_ETIQUETA);
            p.texto("FECHA PROCESO:", fEtiqueta, TEXTO, 58, 72, 100, 14, IZQ);
            p.texto(fechaLarga(d.fechaProceso()), fuente(true, T_FECHA), TEXTO, 161, 72, 60, 14, IZQ);

            // TOTAL A PROCESAR
            p.texto("TOTAL A PROCESAR:", fEtiqueta, TEXTO, 328, 52, 110, 14, IZQ);

            double x0 = 480, x1 = 541.5, x2 = 592.5, x3 = 692.5;
            Font fMini = fuente(true, T_MINI);
            p.rellenar(x0, 43, x3 - x0, 11, MORADO);
            p.lineaV(x1, 43, 54, MORADO_LINEA, 0.6);
            p.lineaV(x2, 43, 54, MORADO_LINEA, 0.6);
            p.texto("JABAS", fMini, BLANCO, x0, 43, x1 - x0, 11, CENTRO);
            p.texto("KILOS", fMini, BLANCO, x1, 43, x2 - x1, 11, CENTRO);
            p.texto("VARIEDAD", fMini, BLANCO, x2, 43, x3 - x2, 11, CENTRO);

            double y = yMiniFilas;
            Font fFecha = fuente(false, 10.4);
            for (LoteReporte l : d.ingresos()) {
                p.rellenar(x0, y, x3 - x0, 10.1, BLANCO);
                p.bordeRect(x0, y, x1 - x0, 10.1, LINEA_SUAVE, 0.6);
                p.bordeRect(x1, y, x2 - x1, 10.1, LINEA_SUAVE, 0.6);
                p.bordeRect(x2, y, x3 - x2, 10.1, LINEA_SUAVE, 0.6);
                p.texto(fmt(l.jabas(), "#,##0"), fMini, TEXTO, x0, y, x1 - x0, 10.1, CENTRO);
                p.texto(fmt(l.kilos(), "#,##0.0"), fMini, TEXTO, x1, y, x2 - x1, 10.1, CENTRO);
                p.texto(mayus(l.variedad()), fMini, TEXTO, x2, y, x3 - x2, 10.1, CENTRO);
                p.texto(fechaCorta(l.fecha()), fFecha, TEXTO, 440, y, 37, 10.1, DER);
                y += 10.1;
            }
            p.rellenar(x0, ySubtotal, x2 - x0, 10.5, LILA_SUBTOTAL);
            p.bordeRect(x0, ySubtotal, x1 - x0, 10.5, LINEA_SUAVE, 0.6);
            p.bordeRect(x1, ySubtotal, x2 - x1, 10.5, LINEA_SUAVE, 0.6);
            p.texto(fmt(mpJabas, "#,##0"), fMini, TEXTO, x0, ySubtotal, x1 - x0, 10.5, CENTRO);
            p.texto(fmt(mpKilos, "#,##0.0"), fMini, TEXTO, x1, ySubtotal, x2 - x1, 10.5, CENTRO);

            // TOTAL M.P / PROCESADO / STOCK
            Font fMp = fuente(true, T_MP);
            Font fMpTenue = fuente(false, T_MINI);
            double h = 10.5;
            double yA = yMp, yB = yMp + 13, yC = yMp + 26;

            p.texto("TOTAL M.P", fMp, TEXTO, 420, yA, 57, h, DER);
            p.rellenar(x0, yA, x2 - x0, h, MORADO);
            p.lineaV(x1, yA, yA + h, MORADO_LINEA, 0.6);
            p.texto(fmt(mpJabas, "#,##0"), fMini, BLANCO, x0, yA, x1 - x0, h, CENTRO);
            p.texto(fmt(mpKilos, "#,##0.00"), fMini, BLANCO, x1, yA, x2 - x1, h, CENTRO);

            p.texto("PROCESADO", fMp, TEXTO, 420, yB, 57, h, DER);
            p.rellenar(x0, yB, x2 - x0, h, BLANCO);
            p.bordeRect(x0, yB, x1 - x0, h, LINEA_SUAVE, 0.6);
            p.bordeRect(x1, yB, x2 - x1, h, LINEA_SUAVE, 0.6);
            p.texto(fmt(totJabas, "#,##0"), fMpTenue, TEXTO_TENUE, x0, yB, x1 - x0, h, CENTRO);
            p.texto(fmt(totKilos, "#,##0.0"), fMpTenue, TEXTO_TENUE, x1, yB, x2 - x1, h, CENTRO);

            p.texto("STOCK", fMp, TEXTO, 420, yC, 57, h, DER);
            p.rellenar(x0, yC, x2 - x0, h, GRIS_STOCK);
            p.bordeRect(x0, yC, x1 - x0, h, LINEA_SUAVE, 0.6);
            p.bordeRect(x1, yC, x2 - x1, h, LINEA_SUAVE, 0.6);
            p.texto(fmt(mpJabas.subtract(totJabas), "#,##0"), fMini, TEXTO, x0, yC, x1 - x0, h, CENTRO);
            p.texto(fmt(mpKilos.subtract(totKilos), "#,##0.0"), fMini, TEXTO, x1, yC, x2 - x1, h, CENTRO);
        }

        private void dibujarTablaPrincipal(Pintor p) {
            Font fEnc = fuente(true, T_ENCABEZADO);
            Font fCuerpo = fuente(false, T_CUERPO);
            Font fHora = fuente(true, T_HORA);
            Font fObs = fuente(false, T_OBS);

            // Encabezado morado
            p.rellenar(C[0], yTabla, C[13] - C[0], ALTO_ENCABEZADO, MORADO);
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                if (i > 0) {
                    p.lineaV(C[i], yTabla, yTabla + ALTO_ENCABEZADO, MORADO_LINEA, 0.6);
                }
                p.lineas(List.of(ENCABEZADOS[i].split("\n")), fEnc, BLANCO, C[i], yTabla, C[i + 1] - C[i], ALTO_ENCABEZADO, 9.4);
            }

            if (bloques.isEmpty()) {
                p.rellenar(C[0], yTabla + ALTO_ENCABEZADO, C[13] - C[0], ALTO_MIN_BLOQUE, LILA);
                p.bordeRect(C[0], yTabla + ALTO_ENCABEZADO, C[13] - C[0], ALTO_MIN_BLOQUE, LINEA, 0.8);
                p.texto("SIN CORTES REGISTRADOS", fCuerpo, TEXTO, C[0], yTabla + ALTO_ENCABEZADO, C[13] - C[0], ALTO_MIN_BLOQUE, CENTRO);
                return;
            }

            for (Bloque b : bloques) {
                double y0 = b.y, y1 = b.y + b.alto;
                p.rellenar(C[0], y0, C[13] - C[0], b.alto, LILA);

                if (b.pausa) {
                    p.texto("ALMUERZO DEL PERSONAL", fuente(true, T_CUERPO), TEXTO, C[0], y0, C[13] - C[0], b.alto, CENTRO);
                    p.lineaV(C[0], y0, y1, LINEA, 0.8);
                    p.lineaV(C[13], y0, y1, LINEA, 0.8);
                    p.lineaH(C[0], C[13], y0, LINEA, 0.8);
                    p.lineaH(C[0], C[13], y1, LINEA, 0.8);
                    continue;
                }

                // Numero de corte, fuera de la tabla
                p.texto(String.valueOf(b.numero), fuente(true, T_CUERPO), TEXTO, 17, y0, 20, b.alto, CENTRO);

                // Celdas combinadas por hora
                CorteReporte c = b.corte;
                p.lineas(List.of(hora(c.inicio()), "A", hora(c.fin())), fHora, TEXTO, C[HORA], y0, C[HORA + 1] - C[HORA], b.alto, 11.5);
                p.texto(fechaCorta(c.fechaCosecha()), fCuerpo, TEXTO, C[FECHA], y0, C[FECHA + 1] - C[FECHA], b.alto, CENTRO);
                p.texto(fmt(c.pesoTotal(), "#,##0.0"), fCuerpo, TEXTO, C[TOTAL_HORA], y0, C[TOTAL_HORA + 1] - C[TOTAL_HORA], b.alto, CENTRO);
                p.texto(fmt(b.acumulado, "#,##0.0"), fCuerpo, TEXTO, C[ACUM], y0, C[ACUM + 1] - C[ACUM], b.alto, CENTRO);

                if (b.lineasObsGeneral != null) {
                    p.lineas(b.lineasObsGeneral, fObs, TEXTO, C[OBS], y0, C[OBS + 1] - C[OBS], b.alto, INTERLINEADO_OBS);
                }

                // Grupos por maquina
                List<Object[]> almuerzos = new ArrayList<>();
                double y = y0;
                for (int gi = 0; gi < b.grupos.size(); gi++) {
                    Grupo g = b.grupos.get(gi);
                    double rowH = g.alto / g.filas.size();

                    // Separador entre maquinas (no en HORA/FECHA/TOTAL/ACUM)
                    if (gi > 0) {
                        p.lineaH(C[VARIEDAD], C[TOTAL_HORA], y, LINEA, 0.8);
                        p.lineaH(C[MAQUINA], C[13], y, LINEA, 0.8);
                    }

                    // VARIEDAD combinada por tramos consecutivos iguales
                    int inicioTramo = 0;
                    for (int i = 1; i <= g.filas.size(); i++) {
                        boolean corta = i == g.filas.size()
                                || !iguales(g.filas.get(i).variedad(), g.filas.get(inicioTramo).variedad());
                        if (corta) {
                            double ty = y + inicioTramo * rowH;
                            double th = (i - inicioTramo) * rowH;
                            p.texto(mayus(g.filas.get(inicioTramo).variedad()), fCuerpo, TEXTO, C[VARIEDAD], ty, C[VARIEDAD + 1] - C[VARIEDAD], th, CENTRO);
                            if (inicioTramo > 0) {
                                p.lineaH(C[VARIEDAD], C[JABAS], ty, LINEA, 0.8);
                            }
                            inicioTramo = i;
                        }
                    }

                    // PESO POR MAQUINA y OBSERVACION combinadas por maquina
                    p.texto(fmt(g.pesoMaquina, "#,##0.0"), fCuerpo, TEXTO, C[PESO_MAQ], y, C[PESO_MAQ + 1] - C[PESO_MAQ], g.alto, CENTRO);
                    if (!g.lineasObs.isEmpty()) {
                        p.lineas(g.lineasObs, fObs, TEXTO, C[OBS], y, C[OBS + 1] - C[OBS], g.alto, INTERLINEADO_OBS);
                    }

                    // Maquina en almuerzo: JABAS y PESO POR VIAJE se combinan en un solo texto
                    if (g.almuerzo) {
                        almuerzos.add(new Object[]{y, g});
                    }

                    // Filas
                    for (int i = 0; i < g.filas.size(); i++) {
                        FilaReporte f = g.filas.get(i);
                        double ry = y + i * rowH;
                        if (i > 0) {
                            if (!g.almuerzo) {
                                p.lineaH(C[JABAS], C[PESO_MAQ], ry, LINEA, 0.8);
                            }
                            p.lineaH(C[MAQUINA], C[OBS], ry, LINEA, 0.8);
                        }
                        if (!g.almuerzo) {
                            p.texto(fmt(f.jabas(), "#,##0"), fCuerpo, TEXTO, C[JABAS], ry, C[JABAS + 1] - C[JABAS], rowH, CENTRO);
                            p.texto(fmt(f.peso(), "0.0"), fCuerpo, TEXTO, C[PESO_VIAJE], ry, C[PESO_VIAJE + 1] - C[PESO_VIAJE], rowH, CENTRO);
                        }
                        p.texto(mayus(f.maquina()), fCuerpo, TEXTO, C[MAQUINA], ry, C[MAQUINA + 1] - C[MAQUINA], rowH, CENTRO);
                        p.texto(mayus(f.kato()), fCuerpo, TEXTO, C[KATO], ry, C[KATO + 1] - C[KATO], rowH, CENTRO);
                        p.texto(String.valueOf(f.empacadores()), fCuerpo, TEXTO, C[EMPAC], ry, C[EMPAC + 1] - C[EMPAC], rowH, CENTRO);
                        p.texto(fmt(f.kgPorEmpacador(), "#,##0") + " Kg", fCuerpo, TEXTO, C[KG_EMP], ry, C[KG_EMP + 1] - C[KG_EMP], rowH, CENTRO);
                    }
                    y += g.alto;
                }

                // Lineas verticales de todas las columnas y horizontales del bloque
                for (double x : C) {
                    p.lineaV(x, y0, y1, LINEA, 0.8);
                }
                p.lineaH(C[0], C[13], y0, LINEA, 0.8);
                p.lineaH(C[0], C[13], y1, LINEA, 0.8);

                // Maquina en almuerzo: JABAS y PESO POR VIAJE son una sola celda combinada,
                // asi que se borra la linea vertical que las separa y se escribe el texto encima.
                for (Object[] a : almuerzos) {
                    double ay = (Double) a[0];
                    Grupo g = (Grupo) a[1];
                    p.rellenar(C[PESO_VIAJE] - 0.7, ay + 0.6, 1.4, g.alto - 1.2, LILA);
                    p.lineas(List.of("ALMUERZO DE PERSONAL", mayus(g.filas.get(0).maquina())), fCuerpo, TEXTO,
                            C[JABAS], ay, C[PESO_MAQ] - C[JABAS], g.alto, 10.5);
                }
            }
        }

        private void dibujarPie(Pintor p) {
            Font f = fuente(true, T_PIE);
            BigDecimal acumuladoPrevio = totJabas.subtract(ultJabas);
            BigDecimal kilosPrevio = totKilos.subtract(ultKilos);

            String[] etiquetas = {"TOTAL", "ACUMULADO", "TOTAL ACUMULADO", "STOCK"};
            String[] jabas = {
                    fmt(ultJabas, "#,##0"), fmt(acumuladoPrevio, "#,##0"),
                    fmt(totJabas, "#,##0"), fmt(mpJabas.subtract(totJabas), "#,##0")};
            String[] kilos = {
                    fmt(ultKilos, "#,##0"), fmt(kilosPrevio, "#,##0"),
                    fmt(totKilos, "#,##0.0"), fmt(mpKilos.subtract(totKilos), "#,##0.00")};

            for (int i = 0; i < 4; i++) {
                double y = yPie + i * ALTO_FILA_PIE;
                p.rellenar(C[FECHA], y, C[PESO_MAQ] - C[FECHA], ALTO_FILA_PIE, MORADO);
                p.lineaV(C[JABAS], y, y + ALTO_FILA_PIE, MORADO_LINEA, 0.6);
                p.lineaV(C[PESO_VIAJE], y, y + ALTO_FILA_PIE, MORADO_LINEA, 0.6);
                if (i > 0) {
                    p.lineaH(C[FECHA], C[PESO_MAQ], y, MORADO_LINEA, 0.6);
                }
                p.texto(etiquetas[i], f, BLANCO, C[FECHA], y, C[JABAS] - C[FECHA], ALTO_FILA_PIE, CENTRO);
                p.texto(jabas[i], f, BLANCO, C[JABAS], y, C[PESO_VIAJE] - C[JABAS], ALTO_FILA_PIE, CENTRO);
                p.texto(kilos[i], f, BLANCO, C[PESO_VIAJE], y, C[PESO_MAQ] - C[PESO_VIAJE], ALTO_FILA_PIE, CENTRO);
            }
        }

        private void dibujarResumen(Pintor p) {
            Font f = fuente(true, T_PIE);
            double xJ = C[FECHA], xK = C[VARIEDAD], xS = C[JABAS], xF = C[PESO_MAQ];
            double y = yResumen;

            // Encabezado lila
            p.rellenar(xJ, y, xF - xJ, 20, LILA);
            p.bordeRect(xJ, y, xK - xJ, 20, LINEA_SUAVE, 0.7);
            p.bordeRect(xK, y, xS - xK, 20, LINEA_SUAVE, 0.7);
            p.bordeRect(xS, y, xF - xS, 20, LINEA_SUAVE, 0.7);
            p.texto("JABAS", f, TEXTO, xJ, y, xK - xJ, 20, CENTRO);
            p.texto("KILOS", f, TEXTO, xK, y, xS - xK, 20, CENTRO);
            p.texto("SUPERVISOR", f, TEXTO, xS, y, xF - xS, 20, CENTRO);
            y += 20;

            for (ResumenMaquina r : resumen) {
                p.rellenar(C[0], y, C[FECHA] - C[0], r.alto, MORADO);
                p.lineaH(C[0], C[FECHA], y, MORADO_LINEA, 0.6);
                p.texto(r.nombre, f, BLANCO, C[0], y, C[FECHA] - C[0], r.alto, CENTRO);

                p.rellenar(xJ, y, xF - xJ, r.alto, BLANCO);
                p.bordeRect(xJ, y, xK - xJ, r.alto, LINEA_SUAVE, 0.7);
                p.bordeRect(xK, y, xS - xK, r.alto, LINEA_SUAVE, 0.7);
                p.bordeRect(xS, y, xF - xS, r.alto, LINEA_SUAVE, 0.7);
                p.texto(fmt(r.jabas, "#,##0.0"), f, TEXTO, xJ, y, xK - xJ, r.alto, CENTRO);
                p.texto(fmt(r.kilos, "#,##0.0"), f, TEXTO, xK, y, xS - xK, r.alto, CENTRO);
                if (!r.lineas.isEmpty()) {
                    p.lineas(r.lineas, f, TEXTO, xS, y, xF - xS, r.alto, 10.6);
                }
                y += r.alto;
            }

            // Total general (solo jabas y kilos)
            BigDecimal tj = BigDecimal.ZERO, tk = BigDecimal.ZERO;
            for (ResumenMaquina r : resumen) {
                tj = tj.add(r.jabas);
                tk = tk.add(r.kilos);
            }
            p.rellenar(xJ, y, xS - xJ, 19.7, LILA);
            p.bordeRect(xJ, y, xK - xJ, 19.7, LINEA_SUAVE, 0.7);
            p.bordeRect(xK, y, xS - xK, 19.7, LINEA_SUAVE, 0.7);
            p.texto(fmt(tj, "#,##0"), f, TEXTO, xJ, y, xK - xJ, 19.7, CENTRO);
            p.texto(fmt(tk, "#,##0.0"), f, TEXTO, xK, y, xS - xK, 19.7, CENTRO);
        }
    }

    // =====================================================================
    // Pintor: texto condensado, lineas y rectangulos
    // =====================================================================

    private static final class Pintor {
        private final Graphics2D g;

        Pintor(Graphics2D g) {
            this.g = g;
        }

        double ancho(String s, Font f) {
            FontMetrics fm = g.getFontMetrics(f);
            double total = 0;
            String[] palabras = s.split(" ", -1);
            for (int i = 0; i < palabras.length; i++) {
                total += fm.getStringBounds(palabras[i], g).getWidth();
                if (i < palabras.length - 1) {
                    total += anchoEspacio(f);
                }
            }
            return total * K;
        }

        /** Texto de una linea, centrado vertical en la celda; se condensa mas si no cabe. */
        void texto(String s, Font f, Color c, double x, double y, double w, double h, int alineacion) {
            if (s == null || s.isEmpty()) {
                return;
            }
            double natural = ancho(s, f);
            double disponible = w - 4;
            double factor = (natural > disponible && disponible > 0) ? disponible / natural : 1.0;
            double tw = natural * factor;
            double px = switch (alineacion) {
                case CENTRO -> x + (w - tw) / 2;
                case DER -> x + w - tw - 2;
                default -> x + 2;
            };
            double base = y + h / 2 + ALTURA_MAYUSCULA * f.getSize2D() / 2;
            dibujar(s, f, c, px, base, K * factor);
        }

        /** Varias lineas centradas horizontal y verticalmente dentro de la celda. */
        void lineas(List<String> ls, Font f, Color c, double x, double y, double w, double h, double interlineado) {
            if (ls.isEmpty()) {
                return;
            }
            double alturaBloque = interlineado * (ls.size() - 1) + ALTURA_MAYUSCULA * f.getSize2D();
            double top = y + (h - alturaBloque) / 2;
            for (int i = 0; i < ls.size(); i++) {
                String s = ls.get(i);
                if (s.isEmpty()) {
                    continue;
                }
                double natural = ancho(s, f);
                double disponible = w - 4;
                double factor = (natural > disponible && disponible > 0) ? disponible / natural : 1.0;
                double px = x + (w - natural * factor) / 2;
                double base = top + ALTURA_MAYUSCULA * f.getSize2D() + i * interlineado;
                dibujar(s, f, c, px, base, K * factor);
            }
        }

        /** Parte el texto en lineas que caben en el ancho dado (respeta los saltos de linea escritos). */
        List<String> ajustar(String texto, Font f, double anchoMax) {
            List<String> resultado = new ArrayList<>();
            for (String parrafo : texto.split("\\R", -1)) {
                String actual = "";
                for (String palabra : parrafo.trim().split("\\s+")) {
                    if (palabra.isEmpty()) {
                        continue;
                    }
                    String candidato = actual.isEmpty() ? palabra : actual + " " + palabra;
                    if (actual.isEmpty() || ancho(candidato, f) <= anchoMax) {
                        actual = candidato;
                    } else {
                        resultado.add(actual);
                        actual = palabra;
                    }
                }
                resultado.add(actual);
            }
            return resultado;
        }

        /** Excel redondea el avance de cada glifo a pixeles enteros y el espacio queda mas ancho que el nominal. */
        private double anchoEspacio(Font f) {
            return 0.32 * f.getSize2D();
        }

        private void dibujar(String s, Font f, Color c, double px, double base, double kx) {
            AffineTransform anterior = g.getTransform();
            g.translate(px, base);
            // Se dibuja a 10x y se reduce: evita que el hinting de tamanos pequenos "coma" los espacios.
            g.scale(kx / 10.0, 0.1);
            g.setFont(f.deriveFont(f.getSize2D() * 10f));
            g.setColor(c);
            FontMetrics fm = g.getFontMetrics(f);
            double x = 0;
            String[] palabras = s.split(" ", -1);
            for (int i = 0; i < palabras.length; i++) {
                g.drawString(palabras[i], (float) (x * 10.0), 0f);
                x += fm.getStringBounds(palabras[i], g).getWidth() + anchoEspacio(f);
            }
            g.setTransform(anterior);
        }

        void rellenar(double x, double y, double w, double h, Color c) {
            g.setColor(c);
            g.fill(new Rectangle2D.Double(x, y, w, h));
        }

        void bordeRect(double x, double y, double w, double h, Color c, double grosor) {
            lineaH(x, x + w, y, c, grosor);
            lineaH(x, x + w, y + h, c, grosor);
            lineaV(x, y, y + h, c, grosor);
            lineaV(x + w, y, y + h, c, grosor);
        }

        void lineaH(double x0, double x1, double y, Color c, double grosor) {
            linea(x0, y, x1, y, c, grosor);
        }

        void lineaV(double x, double y0, double y1, Color c, double grosor) {
            linea(x, y0, x, y1, c, grosor);
        }

        private void linea(double x0, double y0, double x1, double y1, Color c, double grosor) {
            g.setColor(c);
            g.setStroke(new BasicStroke((float) grosor, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g.draw(new Line2D.Double(x0, y0, x1, y1));
        }

        /** Dibuja la imagen ajustada dentro del recuadro, sin deformarla. Ignora null o bytes invalidos. */
        void imagen(byte[] bytes, double x, double y, double w, double h) {
            if (bytes == null || bytes.length == 0) {
                return;
            }
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img == null) {
                    return;
                }
                double escala = Math.min(w / img.getWidth(), h / img.getHeight());
                double iw = img.getWidth() * escala, ih = img.getHeight() * escala;
                AffineTransform t = new AffineTransform();
                t.translate(x + (w - iw) / 2, y + (h - ih) / 2);
                t.scale(escala, escala);
                g.drawImage(img, t, null);
            } catch (IOException ignorada) {
                // Sin logo/foto el reporte se genera igual
            }
        }
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private static void configurar(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private static Font cargarFuente(String archivo, int estilo) {
        try (InputStream in = ReporteExcelRenderer.class.getResourceAsStream("/reporte/fonts/" + archivo)) {
            if (in != null) {
                return Font.createFont(Font.TRUETYPE_FONT, in);
            }
        } catch (Exception ignorada) {
            // se usa la fuente del sistema
        }
        return new Font(Font.SANS_SERIF, estilo, 12);
    }

    private static synchronized Font fuente(boolean negrita, double tamano) {
        String clave = (negrita ? "b" : "r") + tamano;
        return CACHE_FUENTES.computeIfAbsent(clave,
                k -> (negrita ? BASE_NEGRITA : BASE_REGULAR).deriveFont((float) tamano));
    }

    private static String fmt(BigDecimal valor, String patron) {
        DecimalFormat df = new DecimalFormat(patron, DecimalFormatSymbols.getInstance(Locale.US));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(valor == null ? BigDecimal.ZERO : valor);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private static boolean iguales(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static String mayus(String s) {
        return s == null ? "" : s.toUpperCase(Locale.forLanguageTag("es-PE"));
    }

    /** "9:40" (sin cero a la izquierda en la hora, como en el Excel). */
    private static String hora(LocalTime t) {
        return t == null ? "" : t.getHour() + ":" + String.format("%02d", t.getMinute());
    }

    /** "7/10/2026". */
    private static String fechaLarga(LocalDate f) {
        return f == null ? "" : f.getDayOfMonth() + "/" + f.getMonthValue() + "/" + f.getYear();
    }

    /** "7-Oct". */
    private static String fechaCorta(LocalDate f) {
        return f == null ? "" : f.getDayOfMonth() + "-" + MESES[f.getMonthValue() - 1];
    }
}
