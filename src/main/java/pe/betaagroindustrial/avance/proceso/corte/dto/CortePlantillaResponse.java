package pe.betaagroindustrial.avance.proceso.corte.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CortePlantillaResponse(
        Long procesoDiarioId,
        Short numeroCorteSugerido,
        LocalTime horaInicio,
        LocalTime horaFin,
        LocalDate fechaCosecha,
        List<VariedadDetalleDTO> variedades,
        List<MaquinaKatoDTO> maquinasKato
) {
}
