package pe.betaagroindustrial.avance.reporte.vivo;

import java.time.Instant;

/** Respuesta del chequeo barato "¿cambio algo?": la web solo baja el avance completo si {@code version} cambio. */
public record AvanceVersionDTO(Long procesoDiarioId, String version, Instant consultadoEn) {
}
