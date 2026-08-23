package pe.betaagroindustrial.avance.proceso.materiaprima;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.common.audit.Auditable;
import pe.betaagroindustrial.avance.proceso.ProcesoDiario;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ingreso acumulado de materia prima por variedad, para el proceso diario.
 *
 * jabas/kilos representan el ACUMULADO reportado por Recepcion hasta el
 * momento (no un plan fijo): cada vez que Recepcion manda una cifra nueva,
 * esta fila se actualiza (upsert) con el valor absoluto mas reciente, no
 * se suma un delta.
 *
 * El peso promedio por jaba (kilos/jabas) se deriva de aqui y se usa para
 * calcular automaticamente el peso de las jabas procesadas por variedad
 * en cada corte, en vez de pesar cada viaje en bascula.
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

    @Column(name = "variedad", nullable = false, length = 80)
    private String variedad;

    @Column(name = "jabas_plan", nullable = false, precision = 10, scale = 0)
    @Builder.Default
    private BigDecimal jabasPlan = BigDecimal.ZERO;

    @Column(name = "kilos_plan", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal kilosPlan = BigDecimal.ZERO;

    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Short orden = 0;

    /**
     * kilos / jabas, redondeado a 3 decimales. Null si jabas es 0 (evita
     * division por cero cuando aun no hay ingreso reportado).
     */
    @Transient
    public BigDecimal getPesoPromedioPorJaba() {
        if (jabasPlan == null || jabasPlan.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return kilosPlan.divide(jabasPlan, 3, RoundingMode.HALF_UP);
    }
}
