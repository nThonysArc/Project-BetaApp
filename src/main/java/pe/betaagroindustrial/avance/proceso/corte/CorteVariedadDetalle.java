package pe.betaagroindustrial.avance.proceso.corte;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.common.audit.Auditable;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "corte_variedad_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorteVariedadDetalle extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corte_id", nullable = false)
    private Corte corte;

    @Column(name = "variedad", nullable = false, length = 80)
    private String variedad;

    @Column(name = "jabas", nullable = false, precision = 10, scale = 0)
    @Builder.Default
    private BigDecimal jabas = BigDecimal.ZERO;

    @Column(name = "peso_por_viaje", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pesoPorViaje = BigDecimal.ZERO;

    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Short orden = 0;

    @Column(name = "cliente_uuid", unique = true)
    private UUID clienteUuid;
}
