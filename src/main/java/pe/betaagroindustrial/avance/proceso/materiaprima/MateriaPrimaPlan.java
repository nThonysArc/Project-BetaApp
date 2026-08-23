package pe.betaagroindustrial.avance.proceso.materiaprima;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.common.audit.Auditable;
import pe.betaagroindustrial.avance.proceso.ProcesoDiario;
import pe.betaagroindustrial.avance.variedad.Variedad;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ingreso acumulado de materia prima por variedad, para el proceso diario.
 * jabas/kilos representan el ACUMULADO reportado por Recepcion hasta el
 * momento (upsert por variedad, no un plan fijo ni un delta).
 */
@Entity
@Table(name = "materia_prima_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MateriaPrimaPlan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proceso_diario_id", nullable = false)
    private ProcesoDiario procesoDiario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variedad_id", nullable = false)
    private Variedad variedad;

    @Column(name = "jabas_plan", nullable = false, precision = 10, scale = 0)
    @Builder.Default
    private BigDecimal jabasPlan = BigDecimal.ZERO;

    @Column(name = "kilos_plan", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal kilosPlan = BigDecimal.ZERO;

    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Short orden = 0;

    @Transient
    public BigDecimal getPesoPromedioPorJaba() {
        if (jabasPlan == null || jabasPlan.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return kilosPlan.divide(jabasPlan, 3, RoundingMode.HALF_UP);
    }
}
