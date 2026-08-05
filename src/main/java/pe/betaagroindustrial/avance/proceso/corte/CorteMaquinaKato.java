package pe.betaagroindustrial.avance.proceso.corte;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.common.audit.Auditable;
import pe.betaagroindustrial.avance.maquina.Maquina;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "corte_maquina_kato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorteMaquinaKato extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corte_id", nullable = false)
    private Corte corte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @Column(name = "kato_nombre", nullable = false, length = 30)
    private String katoNombre;

    @Column(name = "empacadores", nullable = false)
    @Builder.Default
    private Short empacadores = 0;

    @Column(name = "kg_por_empacador", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal kgPorEmpacador = BigDecimal.ZERO;

    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Short orden = 0;

    @Column(name = "cliente_uuid", unique = true)
    private UUID clienteUuid;
}
