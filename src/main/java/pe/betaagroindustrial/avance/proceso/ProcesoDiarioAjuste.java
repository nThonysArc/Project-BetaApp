package pe.betaagroindustrial.avance.proceso;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.usuario.Usuario;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Tabla de eventos append-only para ajustes manuales sobre los totales
 * acumulados/stock de un proceso diario. El ajuste vigente es siempre
 * el mas reciente (mayor ajustadoEn) para un proceso_diario_id dado.
 *
 * Nunca se hace UPDATE sobre un ajuste existente: cada correccion es
 * un nuevo registro, preservando el historial completo de por que
 * y cuando cambio el numero.
 */
@Entity
@Table(name = "proceso_diario_ajuste")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesoDiarioAjuste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proceso_diario_id", nullable = false)
    private ProcesoDiario procesoDiario;

    @Column(name = "jabas_procesado_ajustado", precision = 10, scale = 0)
    private BigDecimal jabasProcesadoAjustado;

    @Column(name = "kilos_procesado_ajustado", precision = 12, scale = 2)
    private BigDecimal kilosProcesadoAjustado;

    @Column(name = "stock_jabas_ajustado", precision = 10, scale = 0)
    private BigDecimal stockJabasAjustado;

    @Column(name = "stock_kilos_ajustado", precision = 12, scale = 2)
    private BigDecimal stockKilosAjustado;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ajustado_por", nullable = false)
    private Usuario ajustadoPor;

    @Column(name = "ajustado_en", nullable = false)
    @Builder.Default
    private OffsetDateTime ajustadoEn = OffsetDateTime.now();
}
