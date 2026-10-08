package pe.betaagroindustrial.avance.proceso.corte.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CorteRequest(
        @NotNull UUID clienteUuid,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        LocalDate fechaCosecha,
        String observacion,

        @NotEmpty @Valid List<CorteDetalleDTO> detalles,

        /**
         * Opcional (compatibilidad con clientes anteriores). Un elemento por
         * maquina del detalle. Si una maquina no aparece aqui se usa el
         * supervisor del corte anterior, o el sugerido de la maquina.
         */
        @Valid List<CorteSupervisorDTO> supervisores,

        BigDecimal jabasTotalAjustado,
        BigDecimal pesoTotalAjustado,
        String motivoAjuste
) {
}
