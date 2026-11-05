package pe.betaagroindustrial.avance.reporte;

import pe.betaagroindustrial.avance.reporte.ReporteDatos.CorteReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.FilaReporte;
import pe.betaagroindustrial.avance.reporte.ReporteDatos.LoteReporte;
import pe.betaagroindustrial.avance.reporte.ReporteExcelRenderer.Bloque;
import pe.betaagroindustrial.avance.reporte.ReporteExcelRenderer.Diseno;
import pe.betaagroindustrial.avance.reporte.ReporteExcelRenderer.Grupo;
import pe.betaagroindustrial.avance.reporte.ReporteExcelRenderer.ResumenMaquina;
import pe.betaagroindustrial.avance.reporte.XlsxMinimo.Estilo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static pe.betaagroindustrial.avance.reporte.ReporteExcelRenderer.*;
import static pe.betaagroindustrial.avance.reporte.XlsxMinimo.ref;

/**
 * Exporta el reporte de avance (Arandano) a un libro de Excel con el MISMO diseno que la imagen:
 * mismas columnas (proporcionales), colores, celdas combinadas, alturas de fila y logo/foto.
 *
 * Los datos base (jabas, peso por viaje, empacadores, materia prima) son valores editables; todo lo
 * calculado (peso por maquina, total por hora, acumulado, totales, stock, kg/empacador y el resumen por
 * maquina) son FORMULAS, para que se pueda corregir un dato en Excel y todo se recalcule.
 * Si el sistema tiene un total ajustado a mano que no coincide con la suma, esa celda queda como valor fijo.
 *
 * El diseno (posiciones y alturas) sale del mismo calculo que la imagen, asi ambos nunca se desfasan.
 */
public final class ReporteXlsxRenderer {

    /** Factor de ampliacion: la imagen usa ~886 unidades de ancho; en Excel se leen mejor mas grandes. */
    private static final double S = 1.4;

    private static final String MORADO = "7030A0", MORADO_LINEA = "542278", LILA = "FBE9F9", LILA_SUB = "EEC1F1",
            GRIS = "EDEDED", LINEA = "343434", SUAVE = "BFBFBF", TEXTO = "121212", TENUE = "464646", BLANCO = "FFFFFF";

    private ReporteXlsxRenderer() {
    }

    private static double alto(double unidades) {
        return unidades * 0.75 * S;
    }

    private static double pt(double tamanoPx) {
        return Math.round(tamanoPx * 0.84 * 2) / 2.0;
    }

    /** Referencia absoluta tipo $J$12 (para los rangos del SUMIF). */
    private static String abs(int fila, int col) {
        String a = ref(fila, col);
        int i = 0;
        while (i < a.length() && Character.isLetter(a.charAt(i))) i++;
        return "$" + a.substring(0, i) + "$" + a.substring(i);
    }

    private static double d(BigDecimal v) {
        return v == null ? 0 : v.doubleValue();
    }

    private static boolean igual(BigDecimal a, BigDecimal b) {
        return a.subtract(b).abs().compareTo(new BigDecimal("0.005")) < 0;
    }

    public static byte[] renderizar(ReporteDatos datos) {
        Diseno dis = disenar(datos);
        XlsxMinimo x = new XlsxMinimo("Avance");

        // ---- Columnas: A = margen (numero de hora); B..N = las 13 columnas de la tabla ----
        x.anchoCol(0, C[0] * S / 7);
        for (int i = 0; i < 13; i++) {
            x.anchoCol(i + 1, (C[i + 1] - C[i]) * S / 7);
        }
        final int cHORA = HORA + 1, cFECHA = FECHA + 1, cVAR = VARIEDAD + 1, cJAB = JABAS + 1, cPV = PESO_VIAJE + 1,
                cPM = PESO_MAQ + 1, cTH = TOTAL_HORA + 1, cACU = ACUM + 1, cMAQ = MAQUINA + 1, cKATO = KATO + 1,
                cEMP = EMPAC + 1, cKG = KG_EMP + 1, cOBS = OBS + 1;

        // ---- Estilos ----
        Estilo base = new Estilo(pt(T_CUERPO), false, TEXTO, null, null, "center", "center", false, null);
        java.util.function.Function<String, Estilo> conFormato = f -> new Estilo(pt(T_CUERPO), false, TEXTO, LILA, LINEA, "center", "center", false, f);

        int sEnc = x.estilo(new Estilo(pt(T_ENCABEZADO), true, BLANCO, MORADO, MORADO_LINEA, "center", "center", true, null));
        int sTitulo = x.estilo(new Estilo(pt(T_TITULO), true, TEXTO, null, null, "center", "center", false, null));
        int sEtiqIzq = x.estilo(new Estilo(pt(T_ETIQUETA), true, TEXTO, null, null, "left", "center", false, null));
        int sEtiqDer = x.estilo(new Estilo(pt(T_MP), true, TEXTO, null, null, "right", "center", false, null));
        int sFechaIng = x.estilo(new Estilo(pt(10.4), false, TEXTO, null, null, "right", "center", false, null));
        int sFechaProc = x.estilo(new Estilo(pt(T_FECHA), true, TEXTO, null, null, "left", "center", false, null));
        int sMiniEnc = x.estilo(new Estilo(pt(T_MINI), true, BLANCO, MORADO, MORADO_LINEA, "center", "center", false, null));
        int sMiniVar = x.estilo(new Estilo(pt(T_MINI), true, TEXTO, BLANCO, SUAVE, "center", "center", false, null));
        int sMiniJab = x.estilo(new Estilo(pt(T_MINI), true, TEXTO, BLANCO, SUAVE, "center", "center", false, "#,##0"));
        int sMiniKil = x.estilo(new Estilo(pt(T_MINI), true, TEXTO, BLANCO, SUAVE, "center", "center", false, "#,##0.0"));
        int sSubJab = x.estilo(new Estilo(pt(T_MINI), true, TEXTO, LILA_SUB, SUAVE, "center", "center", false, "#,##0"));
        int sSubKil = x.estilo(new Estilo(pt(T_MINI), true, TEXTO, LILA_SUB, SUAVE, "center", "center", false, "#,##0.0"));
        int sMpJab = x.estilo(new Estilo(pt(T_MINI), true, BLANCO, MORADO, MORADO_LINEA, "center", "center", false, "#,##0"));
        int sMpKil = x.estilo(new Estilo(pt(T_MINI), true, BLANCO, MORADO, MORADO_LINEA, "center", "center", false, "#,##0.00"));
        int sProcJab = x.estilo(new Estilo(pt(T_MINI), false, TENUE, BLANCO, SUAVE, "center", "center", false, "#,##0"));
        int sProcKil = x.estilo(new Estilo(pt(T_MINI), false, TENUE, BLANCO, SUAVE, "center", "center", false, "#,##0.0"));
        int sStockJab = x.estilo(new Estilo(pt(T_MINI), true, TEXTO, GRIS, SUAVE, "center", "center", false, "#,##0"));
        int sStockKil = x.estilo(new Estilo(pt(T_MINI), true, TEXTO, GRIS, SUAVE, "center", "center", false, "#,##0.0"));

        int sNumHora = x.estilo(new Estilo(pt(T_CUERPO), true, TEXTO, null, null, "center", "center", false, null));
        int sHora = x.estilo(new Estilo(pt(T_HORA), true, TEXTO, LILA, LINEA, "center", "center", true, null));
        int sTxt = x.estilo(conFormato.apply(null));
        int sJabas = x.estilo(conFormato.apply("#,##0"));
        int sPeso = x.estilo(conFormato.apply("0.0"));
        int sPesoMiles = x.estilo(conFormato.apply("#,##0.0"));
        int sKg = x.estilo(conFormato.apply("#,##0\" Kg\""));
        int sEnteroCentro = x.estilo(conFormato.apply("0"));
        int sObs = x.estilo(new Estilo(pt(T_OBS), false, TEXTO, LILA, LINEA, "center", "center", true, null));
        int sAlmuerzoGrupo = x.estilo(new Estilo(pt(T_CUERPO), false, TEXTO, LILA, LINEA, "center", "center", true, null));
        int sPausa = x.estilo(new Estilo(pt(T_CUERPO), true, TEXTO, LILA, LINEA, "center", "center", false, null));

        int sPieTxt = x.estilo(new Estilo(pt(T_PIE), true, BLANCO, MORADO, MORADO_LINEA, "center", "center", false, null));
        int sPie0 = x.estilo(new Estilo(pt(T_PIE), true, BLANCO, MORADO, MORADO_LINEA, "center", "center", false, "#,##0"));
        int sPie1 = x.estilo(new Estilo(pt(T_PIE), true, BLANCO, MORADO, MORADO_LINEA, "center", "center", false, "#,##0.0"));
        int sPie2 = x.estilo(new Estilo(pt(T_PIE), true, BLANCO, MORADO, MORADO_LINEA, "center", "center", false, "#,##0.00"));

        int sResEnc = x.estilo(new Estilo(pt(T_PIE), true, TEXTO, LILA, SUAVE, "center", "center", false, null));
        int sResNom = x.estilo(new Estilo(pt(T_PIE), true, BLANCO, MORADO, MORADO_LINEA, "center", "center", false, null));
        int sResNum = x.estilo(new Estilo(pt(T_PIE), true, TEXTO, BLANCO, SUAVE, "center", "center", false, "#,##0.0"));
        int sResSup = x.estilo(new Estilo(pt(T_PIE), true, TEXTO, BLANCO, SUAVE, "center", "center", true, null));
        int sResTot0 = x.estilo(new Estilo(pt(T_PIE), true, TEXTO, LILA, SUAVE, "center", "center", false, "#,##0"));
        int sResTot1 = x.estilo(new Estilo(pt(T_PIE), true, TEXTO, LILA, SUAVE, "center", "center", false, "#,##0.0"));

        int r = 0;          // fila actual (base 0)
        double yAcum = 0;   // para rellenar hasta donde empieza la tabla

        // ================= Encabezado superior =================
        x.altoFila(r, alto(43));
        x.combinar(r, cVAR, r, cKG, sTitulo);
        x.texto(r, cVAR, mayus(datos.titulo()), sTitulo);
        x.imagen(datos.logo(), 1, 0, (42 - C[0]) * S, 18 * S, 95 * S, 49 * S);
        x.imagen(datos.fotoProducto(), cOBS, 0, (788 - C[12]) * S, 21 * S, 86 * S, 55 * S);
        r++; yAcum += 43;

        x.altoFila(r, alto(11.5));
        x.texto(r, cMAQ, "JABAS", sMiniEnc);
        x.texto(r, cKATO, "KILOS", sMiniEnc);
        x.combinar(r, cEMP, r, cKG, sMiniEnc);
        x.texto(r, cEMP, "VARIEDAD", sMiniEnc);
        r++; yAcum += 11.5;

        int ingIni = r;
        int n = datos.ingresos().size();
        for (int i = 0; i < n; i++) {
            LoteReporte l = datos.ingresos().get(i);
            x.altoFila(r, alto(10.1));
            if (i == 0) {
                x.texto(r, cPM - 1, "TOTAL A PROCESAR:", sEtiqIzq); // columna F (318): como en la imagen
            }
            x.texto(r, cACU, fechaCorta(l.fecha()), sFechaIng);
            x.numero(r, cMAQ, d(l.jabas()), sMiniJab);
            x.numero(r, cKATO, d(l.kilos()), sMiniKil);
            x.combinar(r, cEMP, r, cKG, sMiniVar);
            x.texto(r, cEMP, mayus(l.variedad()), sMiniVar);
            r++; yAcum += 10.1;
        }
        int ingFin = r - 1;

        int filaSub = r;
        x.altoFila(r, alto(10.5));
        if (n > 0) {
            x.formula(r, cMAQ, "SUM(" + ref(ingIni, cMAQ) + ":" + ref(ingFin, cMAQ) + ")", d(dis.mpJabas), sSubJab);
            x.formula(r, cKATO, "SUM(" + ref(ingIni, cKATO) + ":" + ref(ingFin, cKATO) + ")", d(dis.mpKilos), sSubKil);
        } else {
            x.numero(r, cMAQ, 0, sSubJab);
            x.numero(r, cKATO, 0, sSubKil);
        }
        r++; yAcum += 10.5;

        x.altoFila(r, alto(7)); r++; yAcum += 7;

        int filaMp = r;
        x.altoFila(r, alto(10.5));
        x.texto(r, 1, "FECHA PROCESO:", sEtiqIzq);
        x.texto(r, cVAR, fechaLarga(datos.fechaProceso()), sFechaProc);
        x.texto(r, cACU, "TOTAL M.P", sEtiqDer);
        x.formula(r, cMAQ, ref(filaSub, cMAQ), d(dis.mpJabas), sMpJab);
        x.formula(r, cKATO, ref(filaSub, cKATO), d(dis.mpKilos), sMpKil);
        r++; yAcum += 10.5;

        x.altoFila(r, alto(2.5)); r++; yAcum += 2.5;
        int filaProc = r;
        x.altoFila(r, alto(10.5));
        x.texto(r, cACU, "PROCESADO", sEtiqDer);
        r++; yAcum += 10.5;                                  // se completa al conocer el pie

        x.altoFila(r, alto(2.5)); r++; yAcum += 2.5;
        int filaStock = r;
        x.altoFila(r, alto(10.5));
        x.texto(r, cACU, "STOCK", sEtiqDer);
        x.formula(r, cMAQ, ref(filaMp, cMAQ) + "-" + ref(filaProc, cMAQ), d(dis.mpJabas.subtract(dis.totJabas)), sStockJab);
        x.formula(r, cKATO, ref(filaMp, cKATO) + "-" + ref(filaProc, cKATO), d(dis.mpKilos.subtract(dis.totKilos)), sStockKil);
        r++; yAcum += 10.5;

        double relleno = dis.yTabla - yAcum;
        if (relleno > 0.5) {
            x.altoFila(r, alto(relleno)); r++;
        }

        // ================= Tabla principal =================
        x.altoFila(r, alto(ALTO_ENCABEZADO));
        for (int i = 0; i < ENCABEZADOS.length; i++) {
            x.texto(r, i + 1, ENCABEZADOS[i], sEnc);
        }
        r++;

        int tIni = r;
        if (dis.bloques.isEmpty()) {
            x.altoFila(r, alto(34));
            x.combinar(r, 1, r, cOBS, sPausa);
            x.texto(r, 1, "SIN CORTES REGISTRADOS", sPausa);
            r++;
        }

        // seguimiento para el pie
        int ultTop = -1, ultBot = -1;
        boolean jabasConsistentes = true;
        String refAcumPrev = null;
        List<String> topsHora = new ArrayList<>();

        for (Bloque b : dis.bloques) {
            CorteReporte c = b.corte;
            if (b.pausa) {
                x.altoFila(r, alto(ALTO_PAUSA));
                x.combinar(r, 1, r, cOBS, sPausa);
                x.texto(r, 1, "ALMUERZO DEL PERSONAL", sPausa);
                r++;
                continue;
            }

            int top = r;
            // Filas por grupo (maquina)
            List<int[]> rangosGrupo = new ArrayList<>();
            BigDecimal sumaJabas = BigDecimal.ZERO, sumaPeso = BigDecimal.ZERO;
            if (b.grupos.isEmpty()) {
                x.altoFila(r, alto(b.alto));
                r++;
            }
            for (Grupo g : b.grupos) {
                int g0 = r;
                double hFila = g.alto / g.filas.size();
                for (int i = 0; i < g.filas.size(); i++) {
                    FilaReporte f = g.filas.get(i);
                    int fr = r;
                    x.altoFila(fr, alto(hFila));
                    if (!g.almuerzo) {
                        x.numero(fr, cJAB, d(f.jabas()), sJabas);
                        // Peso = jabas x peso promedio de la variedad en MP (como lo calcula el formulario).
                        // Se formula solo si coincide con lo guardado; si no (variedades mezcladas "A/B",
                        // ajuste manual, sin MP) queda el valor tal cual.
                        BigDecimal jb = nz(f.jabas());
                        BigDecimal kMp = BigDecimal.ZERO, jMp = BigDecimal.ZERO;
                        boolean mezclada = f.variedad() != null && f.variedad().contains("/");
                        for (LoteReporte l : datos.ingresos()) {
                            if (!mezclada && f.variedad() != null && f.variedad().trim().equalsIgnoreCase(l.variedad() == null ? "" : l.variedad().trim())) {
                                kMp = kMp.add(nz(l.kilos()));
                                jMp = jMp.add(nz(l.jabas()));
                            }
                        }
                        boolean formulable = !mezclada && jMp.signum() > 0 && jb.signum() > 0
                                && igual(jb.multiply(kMp).divide(jMp, 2, java.math.RoundingMode.HALF_UP), nz(f.peso()));
                        if (formulable) {
                            String rVar = abs(ingIni, cEMP) + ":" + abs(ingFin, cEMP);
                            String rKil = abs(ingIni, cKATO) + ":" + abs(ingFin, cKATO);
                            String rJab = abs(ingIni, cMAQ) + ":" + abs(ingFin, cMAQ);
                            String vr = "\"" + mayus(f.variedad()).replace("\"", "\"\"") + "\""; // la celda de variedad esta combinada: se usa el texto
                            x.formula(fr, cPV, "ROUND(" + ref(fr, cJAB) + "*SUMIF(" + rVar + "," + vr + "," + rKil + ")/SUMIF("
                                    + rVar + "," + vr + "," + rJab + "),2)", d(f.peso()), sPeso);
                        } else {
                            x.numero(fr, cPV, d(f.peso()), sPeso);
                        }
                    }
                    x.texto(fr, cMAQ, mayus(f.maquina()), sTxt);
                    x.texto(fr, cKATO, mayus(f.kato()), sTxt);
                    x.numero(fr, cEMP, f.empacadores(), sEnteroCentro);
                    BigDecimal peso = nz(f.peso());
                    BigDecimal calc = f.empacadores() > 0
                            ? peso.divide(BigDecimal.valueOf(f.empacadores()), 4, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    if (igual(calc, nz(f.kgPorEmpacador()))) {
                        x.formula(fr, cKG, "IF(" + ref(fr, cEMP) + ">0," + ref(fr, cPV) + "/" + ref(fr, cEMP) + ",0)", d(calc), sKg);
                    } else {
                        x.numero(fr, cKG, d(f.kgPorEmpacador()), sKg); // valor puesto a mano en el sistema
                    }
                    sumaJabas = sumaJabas.add(nz(f.jabas()));
                    sumaPeso = sumaPeso.add(peso);
                    r++;
                }
                int g1 = r - 1;
                rangosGrupo.add(new int[]{g0, g1});

                // PESO POR MAQUINA (combinada por grupo)
                x.combinar(g0, cPM, g1, cPM, sPesoMiles);
                x.formula(g0, cPM, "SUM(" + ref(g0, cPV) + ":" + ref(g1, cPV) + ")", d(g.pesoMaquina), sPesoMiles);

                // VARIEDAD combinada por tramos consecutivos iguales
                int ini = 0;
                for (int i = 1; i <= g.filas.size(); i++) {
                    if (i == g.filas.size() || !iguales(g.filas.get(i).variedad(), g.filas.get(ini).variedad())) {
                        x.combinar(g0 + ini, cVAR, g0 + i - 1, cVAR, sTxt);
                        x.texto(g0 + ini, cVAR, mayus(g.filas.get(ini).variedad()), sTxt);
                        ini = i;
                    }
                }

                if (g.almuerzo) {
                    x.combinar(g0, cJAB, g1, cPV, sAlmuerzoGrupo);
                    x.texto(g0, cJAB, "ALMUERZO DE PERSONAL\n" + mayus(g.filas.get(0).maquina()), sAlmuerzoGrupo);
                }

                // OBSERVACION por maquina (si no hay observacion general)
                if (b.lineasObsGeneral == null) {
                    x.combinar(g0, cOBS, g1, cOBS, sObs);
                    if (!vacio(g.observacion)) {
                        x.texto(g0, cOBS, mayus(g.observacion), sObs);
                    }
                }
            }
            int bot = r - 1;

            // OBSERVACION general: una sola celda para toda la hora
            if (b.lineasObsGeneral != null) {
                x.combinar(top, cOBS, bot, cOBS, sObs);
                x.texto(top, cOBS, mayus(c.observacionGeneral()), sObs);
            } else if (b.grupos.isEmpty()) {
                x.combinar(top, cOBS, bot, cOBS, sObs);
            }

            // Numero de hora (margen), HORA y FECHA combinadas por bloque
            x.combinar(top, 0, bot, 0, sNumHora);
            x.texto(top, 0, String.valueOf(b.numero), sNumHora);
            x.combinar(top, cHORA, bot, cHORA, sHora);
            x.texto(top, cHORA, hora(c.inicio()) + "\nA\n" + hora(c.fin()), sHora);
            x.combinar(top, cFECHA, bot, cFECHA, sTxt);
            x.texto(top, cFECHA, fechaCorta(c.fechaCosecha()), sTxt);

            // TOTAL POR HORA y ACUMULADO (combinadas por bloque)
            x.combinar(top, cTH, bot, cTH, sPesoMiles);
            if (igual(sumaPeso, nz(c.pesoTotal()))) {
                x.formula(top, cTH, "SUM(" + ref(top, cPV) + ":" + ref(bot, cPV) + ")", d(c.pesoTotal()), sPesoMiles);
            } else {
                x.numero(top, cTH, d(c.pesoTotal()), sPesoMiles); // total ajustado a mano en el sistema
            }
            x.combinar(top, cACU, bot, cACU, sPesoMiles);
            String formAcum = refAcumPrev == null ? ref(top, cTH) : refAcumPrev + "+" + ref(top, cTH);
            x.formula(top, cACU, formAcum, d(b.acumulado), sPesoMiles);
            refAcumPrev = ref(top, cACU);

            topsHora.add(ref(top, cTH));
            if (!igual(sumaJabas, nz(c.jabasTotal()))) {
                jabasConsistentes = false;
            }
            ultTop = top;
            ultBot = bot;
        }
        int tFin = r - 1;

        // ================= Pie de totales =================
        String[] etiquetas = {"TOTAL", "ACUMULADO", "TOTAL ACUMULADO", "STOCK"};
        int pie0 = r;
        for (int i = 0; i < 4; i++) {
            x.altoFila(r + i, alto(ALTO_FILA_PIE));
            x.combinar(r + i, cFECHA, r + i, cVAR, sPieTxt);
            x.texto(r + i, cFECHA, etiquetas[i], sPieTxt);
        }
        BigDecimal acumPrevJ = dis.totJabas.subtract(dis.ultJabas), acumPrevK = dis.totKilos.subtract(dis.ultKilos);
        int sTotJ = sPie0;
        // TOTAL (ultima hora de produccion)
        if (ultTop >= 0 && jabasConsistentes) {
            x.formula(pie0, cJAB, "SUM(" + ref(ultTop, cJAB) + ":" + ref(ultBot, cJAB) + ")", d(dis.ultJabas), sTotJ);
        } else {
            x.numero(pie0, cJAB, d(dis.ultJabas), sTotJ);
        }
        if (ultTop >= 0) {
            x.formula(pie0, cPV, ref(ultTop, cTH), d(dis.ultKilos), sPie0);
        } else {
            x.numero(pie0, cPV, 0, sPie0);
        }
        // TOTAL ACUMULADO
        int pieTA = pie0 + 2;
        if (jabasConsistentes && tFin >= tIni && !dis.bloques.isEmpty()) {
            x.formula(pieTA, cJAB, "SUM(" + ref(tIni, cJAB) + ":" + ref(tFin, cJAB) + ")", d(dis.totJabas), sPie0);
        } else {
            x.numero(pieTA, cJAB, d(dis.totJabas), sPie0);
        }
        if (!topsHora.isEmpty()) {
            x.formula(pieTA, cPV, "SUM(" + String.join(",", topsHora) + ")", d(dis.totKilos), sPie1);
        } else {
            x.numero(pieTA, cPV, 0, sPie1);
        }
        // ACUMULADO (lo anterior a la ultima hora)
        x.formula(pie0 + 1, cJAB, ref(pieTA, cJAB) + "-" + ref(pie0, cJAB), d(acumPrevJ), sPie0);
        x.formula(pie0 + 1, cPV, ref(pieTA, cPV) + "-" + ref(pie0, cPV), d(acumPrevK), sPie0);
        // STOCK
        x.formula(pie0 + 3, cJAB, ref(filaMp, cMAQ) + "-" + ref(pieTA, cJAB), d(dis.mpJabas.subtract(dis.totJabas)), sPie0);
        x.formula(pie0 + 3, cPV, ref(filaMp, cKATO) + "-" + ref(pieTA, cPV), d(dis.mpKilos.subtract(dis.totKilos)), sPie2);
        r += 4;

        // PROCESADO del encabezado = TOTAL ACUMULADO del pie
        x.formula(filaProc, cMAQ, ref(pieTA, cJAB), d(dis.totJabas), sProcJab);
        x.formula(filaProc, cKATO, ref(pieTA, cPV), d(dis.totKilos), sProcKil);

        // ================= Resumen por maquina =================
        x.altoFila(r, alto(21)); r++;
        x.altoFila(r, alto(20));
        x.texto(r, cFECHA, "JABAS", sResEnc);
        x.texto(r, cVAR, "KILOS", sResEnc);
        x.combinar(r, cJAB, r, cPV, sResEnc);
        x.texto(r, cJAB, "SUPERVISOR", sResEnc);
        r++;
        int resIni = r;
        double totJ = 0, totK = 0;
        int rFin = Math.max(tFin, tIni);
        String rangoMaq = abs(tIni, cMAQ) + ":" + abs(rFin, cMAQ);
        String rangoJab = abs(tIni, cJAB) + ":" + abs(rFin, cJAB);
        String rangoPv = abs(tIni, cPV) + ":" + abs(rFin, cPV);
        for (ResumenMaquina rm : dis.resumen) {
            x.altoFila(r, alto(rm.alto));
            x.texto(r, cHORA, rm.nombre, sResNom);
            String nombre = abs(r, cHORA);
            x.formula(r, cFECHA, "SUMIF(" + rangoMaq + "," + nombre + "," + rangoJab + ")", d(rm.jabas), sResNum);
            x.formula(r, cVAR, "SUMIF(" + rangoMaq + "," + nombre + "," + rangoPv + ")", d(rm.kilos), sResNum);
            x.combinar(r, cJAB, r, cPV, sResSup);
            if (!rm.lineas.isEmpty()) {
                x.texto(r, cJAB, String.join("\n", rm.lineas), sResSup);
            }
            totJ += d(rm.jabas);
            totK += d(rm.kilos);
            r++;
        }
        x.altoFila(r, alto(19.7));
        if (!dis.resumen.isEmpty()) {
            x.formula(r, cFECHA, "SUM(" + ref(resIni, cFECHA) + ":" + ref(r - 1, cFECHA) + ")", totJ, sResTot0);
            x.formula(r, cVAR, "SUM(" + ref(resIni, cVAR) + ":" + ref(r - 1, cVAR) + ")", totK, sResTot1);
        } else {
            x.numero(r, cFECHA, 0, sResTot0);
            x.numero(r, cVAR, 0, sResTot1);
        }

        return x.generar();
    }
}
