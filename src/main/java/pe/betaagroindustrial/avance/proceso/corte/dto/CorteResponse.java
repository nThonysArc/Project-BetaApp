package pe.betaagroindustrial.avance.proceso.corte.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CorteResponse(
        Long id,
        Long procesoDiarioId,
        Short numeroCorte,
        LocalTime horaInicio,
        LocalTime horaFin,
        LocalDate fechaCosecha,
        String observacion,

        BigDecimal jabasTotalCalculado,
        BigDecimal jabasTotalAjustado,
        BigDecimal jabasTotalEfectivo,

        BigDecimal pesoTotalCalculado,
        BigDecimal pesoTotalAjustado,
        BigDecimal pesoTotalEfectivo,

        String estado,
        OffsetDateTime consolidadoEn,
        boolean requiereRevision,
        UUID clienteUuid,

        List<VariedadDetalleDTO> variedades,
        List<MaquinaKatoDTO> maquinasKato
) {
}
