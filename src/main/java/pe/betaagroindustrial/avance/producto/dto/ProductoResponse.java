package pe.betaagroindustrial.avance.producto.dto;

import pe.betaagroindustrial.avance.producto.PlantillaReporte;

public record ProductoResponse(
        Long id,
        String nombre,
        String unidadMedida,
        PlantillaReporte plantillaReporte,
        boolean activo
) {
}
