package pe.betaagroindustrial.avance.reporte.vivo;

import java.time.Instant;

/**
 * Sobre que recibe la web. {@code avance} es null cuando la plantilla del producto aun no tiene vista
 * en vivo (hoy: Vid); en ese caso {@code disponible}=false y {@code mensaje} explica por que, para que
 * la pantalla lo diga en vez de fallar.
 */
public record AvanceVivoResponse(
        Long procesoDiarioId,
        String plantilla,
        String estadoProceso,
        boolean disponible,
        String mensaje,
        String version,
        Instant generadoEn,
        AvanceVivo avance
) {
}
