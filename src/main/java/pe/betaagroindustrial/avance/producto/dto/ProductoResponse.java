package pe.betaagroindustrial.avance.producto.dto;

public record ProductoResponse(
        Long id,
        String nombre,
        String unidadMedida,
        boolean activo
) {
}
