package pe.betaagroindustrial.avance.producto;

/**
 * Formato del reporte de avance por hora que usa un producto. Cada producto elige su plantilla
 * (patron Strategy): agregar un formato nuevo es agregar un valor aqui y su renderer,
 * sin tocar el resto del flujo de cortes.
 */
public enum PlantillaReporte {
    /** Reporte por maquina + kato (kilos y kg por empacador). */
    ARANDANO,
    /** "Avance por hora de seleccion": modulo, personas, jabas y rendimiento por persona. */
    VID
}
