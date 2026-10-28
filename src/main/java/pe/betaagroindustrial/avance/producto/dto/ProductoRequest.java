package pe.betaagroindustrial.avance.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pe.betaagroindustrial.avance.producto.PlantillaReporte;

public record ProductoRequest(
        @NotBlank @Size(max = 80) String nombre,
        @Size(max = 20) String unidadMedida,
        PlantillaReporte plantillaReporte
) {
}
