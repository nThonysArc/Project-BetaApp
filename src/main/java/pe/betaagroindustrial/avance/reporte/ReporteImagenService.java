package pe.betaagroindustrial.avance.reporte;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.betaagroindustrial.avance.common.exception.ResourceNotFoundException;
import pe.betaagroindustrial.avance.proceso.ProcesoDiario;
import pe.betaagroindustrial.avance.proceso.ProcesoDiarioRepository;
import pe.betaagroindustrial.avance.proceso.corte.Corte;
import pe.betaagroindustrial.avance.proceso.corte.CorteDetalle;
import pe.betaagroindustrial.avance.proceso.corte.CorteRepository;
import pe.betaagroindustrial.avance.proceso.materiaprima.MateriaPrimaPlan;
import pe.betaagroindustrial.avance.proceso.materiaprima.MateriaPrimaPlanRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera la imagen del reporte de avance del dia (reemplaza el screenshot
 * manual de la plantilla de Excel). Usa Java2D/Graphics2D directamente -
 * sin navegador headless ni dependencias nuevas - para mantener el build
 * y el despliegue simples.
 *
 * La imagen se genera BAJO DEMANDA a partir de los datos ya guardados
 * (ProcesoDiario + sus Cortes + MateriaPrimaPlan), nunca se persiste -
 * asi siempre refleja el estado mas reciente.
 */
@Service
@RequiredArgsConstructor
public class ReporteImagenService {

    private static final Color COLOR_MORADO = new Color(112, 48, 160);
    private static final Color COLOR_MORADO_CLARO = new Color(230, 220, 245);
    private static final Color COLOR_GRIS_CLARO = new Color(245, 245, 245);
    private static final Color COLOR_BORDE = new Color(180, 180, 180);
    private static final Color COLOR_TEXTO = new Color(30, 30, 30);

    private static final int ANCHO = 1300;
    private static final int MARGEN = 32;
    private static final int ALTO_FILA = 26;
    private static final Font FUENTE_TITULO = new Font("SansSerif", Font.BOLD, 22);
    private static final Font FUENTE_SUBTITULO = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font FUENTE_SECCION = new Font("SansSerif", Font.BOLD, 15);
    private static final Font FUENTE_HEADER = new Font("SansSerif", Font.BOLD, 12);
    private static final Font FUENTE_CELDA = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font FUENTE_CELDA_BOLD = new Font("SansSerif", Font.BOLD, 12);

    private final ProcesoDiarioRepository procesoDiarioRepository;
    private final CorteRepository corteRepository;
    private final MateriaPrimaPlanRepository materiaPrimaPlanRepository;

    @Transactional(readOnly = true)
    public byte[] generarPng(Long procesoDiarioId) {
        ProcesoDiario proceso = procesoDiarioRepository.findById(procesoDiarioId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcesoDiario", procesoDiarioId));

        List<MateriaPrimaPlan> ingresos = materiaPrimaPlanRepository
                .findByProcesoDiarioIdOrderByOrdenAsc(procesoDiarioId);

        List<Corte> cortes = corteRepository
                .findByProcesoDiarioIdOrderByNumeroCorteAsc(procesoDiarioId);

        // ---- Calcular la altura total necesaria antes de dibujar ----
        int filasIngreso = Math.max(ingresos.size(), 1);
        int filasDetalle = cortes.stream().mapToInt(c -> Math.max(c.getDetalles().size(), 1)).sum();
        int filasSubtotalCorte = cortes.size();
        Map<String, BigDecimal> pesoPorMaquina = calcularPesoPorMaquina(cortes);
        int filasResumenMaquina = Math.max(pesoPorMaquina.size(), 1);

        int alto = 150                                  // encabezado
                + 40 + (filasIngreso + 1) * ALTO_FILA    // seccion ingreso (+header)
                + 40 + (filasDetalle + filasSubtotalCorte + 1) * ALTO_FILA // seccion cortes (+header)
                + 40 + (filasResumenMaquina + 1) * ALTO_FILA // resumen por maquina
                + 60;                                    // total del dia + margen final

        BufferedImage imagen = new BufferedImage(ANCHO, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imagen.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, ANCHO, alto);

        int y = MARGEN;
        y = dibujarEncabezado(g2d, proceso, y);
        y = dibujarSeccionIngreso(g2d, ingresos, y);
        y = dibujarSeccionCortes(g2d, cortes, y);
        dibujarResumenMaquinas(g2d, pesoPorMaquina, cortes, y);

        g2d.dispose();

        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Error al generar la imagen del reporte", e);
        }
    }

    // ==========================================================
    // Secciones del reporte
    // ==========================================================

    private int dibujarEncabezado(Graphics2D g2d, ProcesoDiario proceso, int y) {
        String producto = proceso.getCampana().getProducto().getNombre();
        String campana = proceso.getCampana().getNombre();
        String fecha = proceso.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        g2d.setColor(COLOR_MORADO);
        g2d.fillRect(0, 0, ANCHO, 100);

        g2d.setColor(Color.WHITE);
        g2d.setFont(FUENTE_TITULO);
        centrarTexto(g2d, "AVANCE DE PRODUCCION - " + producto.toUpperCase(), ANCHO / 2, 42);

        g2d.setFont(FUENTE_SUBTITULO);
        centrarTexto(g2d, campana + "  |  Fecha: " + fecha + "  |  Estado: " + proceso.getEstado(), ANCHO / 2, 68);

        g2d.setColor(COLOR_TEXTO);
        return 130;
    }

    private int dibujarSeccionIngreso(Graphics2D g2d, List<MateriaPrimaPlan> ingresos, int y) {
        g2d.setFont(FUENTE_SECCION);
        g2d.setColor(COLOR_TEXTO);
        g2d.drawString("Ingreso de materia prima (acumulado reportado por Recepcion)", MARGEN, y);
        y += 24;

        String[] columnas = {"Variedad", "Jabas", "Kilos", "Peso promedio/jaba (kg)"};
        int[] anchos = {400, 250, 250, 400};

        y = dibujarFilaHeader(g2d, columnas, anchos, MARGEN, y);

        if (ingresos.isEmpty()) {
            y = dibujarFila(g2d, new String[]{"Sin ingreso registrado todavia", "-", "-", "-"}, anchos, MARGEN, y, false);
        } else {
            for (MateriaPrimaPlan i : ingresos) {
                BigDecimal promedio = i.getPesoPromedioPorJaba();
                y = dibujarFila(g2d, new String[]{
                        i.getVariedad().getNombre(),
                        formatEntero(i.getJabasPlan()),
                        formatDecimal(i.getKilosPlan()),
                        promedio != null ? formatDecimal(promedio) : "-"
                }, anchos, MARGEN, y, false);
            }
        }

        return y + 16;
    }

    private int dibujarSeccionCortes(Graphics2D g2d, List<Corte> cortes, int y) {
        g2d.setFont(FUENTE_SECCION);
        g2d.setColor(COLOR_TEXTO);
        g2d.drawString("Detalle por hora, maquina, kato y variedad", MARGEN, y);
        y += 24;

        String[] columnas = {"Hora", "Maquina", "Kato", "Variedad", "Jabas", "Peso (kg)", "Empac.", "Kg/Emp"};
        int[] anchos = {150, 180, 130, 200, 130, 150, 130, 140};

        y = dibujarFilaHeader(g2d, columnas, anchos, MARGEN, y);

        BigDecimal acumJabas = BigDecimal.ZERO;
        BigDecimal acumPeso = BigDecimal.ZERO;

        for (Corte corte : cortes) {
            String rangoHora = corte.getHoraInicio() + " - " + corte.getHoraFin();

            List<CorteDetalle> detalles = corte.getDetalles();
            if (detalles.isEmpty()) {
                y = dibujarFila(g2d, new String[]{rangoHora, "-", "-", "-", "-", "-", "-", "-"}, anchos, MARGEN, y, false);
            } else {
                for (CorteDetalle d : detalles) {
                    y = dibujarFila(g2d, new String[]{
                            rangoHora,
                            d.getMaquina().getNombre(),
                            d.getMaquinaKato().getNombre(),
                            d.getVariedad().getNombre(),
                            formatEntero(d.getJabas()),
                            formatDecimal(d.getPesoTotal()),
                            String.valueOf(d.getEmpacadores()),
                            formatDecimal(d.getKgPorEmpacador())
                    }, anchos, MARGEN, y, false);
                }
            }

            acumJabas = acumJabas.add(corte.getJabasTotalEfectivo() != null ? corte.getJabasTotalEfectivo() : BigDecimal.ZERO);
            acumPeso = acumPeso.add(corte.getPesoTotalEfectivo() != null ? corte.getPesoTotalEfectivo() : BigDecimal.ZERO);

            String etiquetaSubtotal = String.format(
                    "Corte #%d (%s): %s jabas, %s kg  |  Acumulado dia: %s jabas, %s kg",
                    corte.getNumeroCorte(), rangoHora,
                    formatEntero(corte.getJabasTotalEfectivo()), formatDecimal(corte.getPesoTotalEfectivo()),
                    formatEntero(acumJabas), formatDecimal(acumPeso)
            );
            y = dibujarFilaSubtotal(g2d, etiquetaSubtotal, MARGEN, y, sumaAnchos(anchos));
        }

        // Total general del dia
        y += 6;
        g2d.setFont(FUENTE_SECCION);
        g2d.setColor(COLOR_MORADO);
        g2d.drawString(String.format("TOTAL DEL DIA: %s jabas | %s kg", formatEntero(acumJabas), formatDecimal(acumPeso)), MARGEN, y);
        g2d.setColor(COLOR_TEXTO);

        return y + 30;
    }

    private void dibujarResumenMaquinas(Graphics2D g2d, Map<String, BigDecimal> pesoPorMaquina, List<Corte> cortes, int y) {
        g2d.setFont(FUENTE_SECCION);
        g2d.setColor(COLOR_TEXTO);
        g2d.drawString("Resumen por maquina", MARGEN, y);
        y += 24;

        String[] columnas = {"Maquina", "Supervisor", "Peso total (kg)"};
        int[] anchos = {400, 400, 400};

        y = dibujarFilaHeader(g2d, columnas, anchos, MARGEN, y);

        if (pesoPorMaquina.isEmpty()) {
            dibujarFila(g2d, new String[]{"Sin cortes registrados", "-", "-"}, anchos, MARGEN, y, false);
            return;
        }

        Map<String, String> supervisorPorMaquina = obtenerSupervisorPorMaquina(cortes);

        for (Map.Entry<String, BigDecimal> entrada : pesoPorMaquina.entrySet()) {
            String supervisor = supervisorPorMaquina.getOrDefault(entrada.getKey(), "-");
            y = dibujarFila(g2d, new String[]{
                    entrada.getKey(), supervisor, formatDecimal(entrada.getValue())
            }, anchos, MARGEN, y, false);
        }
    }

    // ==========================================================
    // Helpers de calculo
    // ==========================================================

    private Map<String, BigDecimal> calcularPesoPorMaquina(List<Corte> cortes) {
        Map<String, BigDecimal> resultado = new LinkedHashMap<>();
        for (Corte corte : cortes) {
            for (CorteDetalle d : corte.getDetalles()) {
                String nombreMaquina = d.getMaquina().getNombre();
                resultado.merge(nombreMaquina, d.getPesoTotal(), BigDecimal::add);
            }
        }
        return resultado;
    }

    private Map<String, String> obtenerSupervisorPorMaquina(List<Corte> cortes) {
        Map<String, String> resultado = new LinkedHashMap<>();
        for (Corte corte : cortes) {
            for (CorteDetalle d : corte.getDetalles()) {
                String nombreMaquina = d.getMaquina().getNombre();
                if (!resultado.containsKey(nombreMaquina)) {
                    resultado.put(nombreMaquina,
                            d.getMaquina().getSupervisor() != null
                                    ? d.getMaquina().getSupervisor().getNombreCompleto()
                                    : "-");
                }
            }
        }
        return resultado;
    }

    // ==========================================================
    // Helpers de dibujo
    // ==========================================================

    private int dibujarFilaHeader(Graphics2D g2d, String[] columnas, int[] anchos, int x, int y) {
        int ancho = sumaAnchos(anchos);
        g2d.setColor(COLOR_MORADO_CLARO);
        g2d.fillRect(x, y, ancho, ALTO_FILA);
        g2d.setColor(COLOR_BORDE);
        g2d.drawRect(x, y, ancho, ALTO_FILA);

        g2d.setFont(FUENTE_HEADER);
        g2d.setColor(COLOR_TEXTO);
        int colX = x;
        for (int i = 0; i < columnas.length; i++) {
            g2d.drawString(columnas[i], colX + 8, y + 17);
            if (i < columnas.length - 1) {
                g2d.setColor(COLOR_BORDE);
                g2d.drawLine(colX + anchos[i], y, colX + anchos[i], y + ALTO_FILA);
            }
            colX += anchos[i];
        }
        return y + ALTO_FILA;
    }

    private int dibujarFila(Graphics2D g2d, String[] valores, int[] anchos, int x, int y, boolean bold) {
        int ancho = sumaAnchos(anchos);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x, y, ancho, ALTO_FILA);
        g2d.setColor(COLOR_BORDE);
        g2d.drawRect(x, y, ancho, ALTO_FILA);

        g2d.setFont(bold ? FUENTE_CELDA_BOLD : FUENTE_CELDA);
        g2d.setColor(COLOR_TEXTO);
        int colX = x;
        for (int i = 0; i < valores.length; i++) {
            g2d.drawString(valores[i], colX + 8, y + 17);
            if (i < valores.length - 1) {
                g2d.setColor(COLOR_BORDE);
                g2d.drawLine(colX + anchos[i], y, colX + anchos[i], y + ALTO_FILA);
            }
            colX += anchos[i];
        }
        return y + ALTO_FILA;
    }

    private int dibujarFilaSubtotal(Graphics2D g2d, String texto, int x, int y, int ancho) {
        g2d.setColor(COLOR_GRIS_CLARO);
        g2d.fillRect(x, y, ancho, ALTO_FILA);
        g2d.setColor(COLOR_BORDE);
        g2d.drawRect(x, y, ancho, ALTO_FILA);

        g2d.setFont(FUENTE_CELDA_BOLD);
        g2d.setColor(COLOR_MORADO);
        g2d.drawString(texto, x + 8, y + 17);
        g2d.setColor(COLOR_TEXTO);

        return y + ALTO_FILA;
    }

    private void centrarTexto(Graphics2D g2d, String texto, int centroX, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int ancho = fm.stringWidth(texto);
        g2d.drawString(texto, centroX - ancho / 2, y);
    }

    private int sumaAnchos(int[] anchos) {
        int total = 0;
        for (int a : anchos) total += a;
        return total;
    }

    private String formatEntero(BigDecimal valor) {
        if (valor == null) return "-";
        return valor.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatDecimal(BigDecimal valor) {
        if (valor == null) return "-";
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
