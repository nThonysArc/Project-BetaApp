package pe.betaagroindustrial.avance.variedad.dto;

public record VariedadResponse(
        Long id,
        Long productoId,
        String nombre,
        Short orden,
        boolean activo
) {
}
