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

/**
 * Corresponde a la tabla "TOTAL A PROCESAR" de la plantilla original:
 * jabas/kilos planificados por variedad para el dia.
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
}
