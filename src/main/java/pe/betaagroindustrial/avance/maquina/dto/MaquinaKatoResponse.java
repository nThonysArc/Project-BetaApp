package pe.betaagroindustrial.avance.maquina.dto;

public record MaquinaKatoResponse(
        Long id,
        Long maquinaId,
        String nombre,
        Short orden,
        boolean activo
) {
}
