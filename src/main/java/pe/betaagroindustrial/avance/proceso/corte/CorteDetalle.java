package pe.betaagroindustrial.avance.proceso.corte;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.common.audit.Auditable;
import pe.betaagroindustrial.avance.maquina.Maquina;
import pe.betaagroindustrial.avance.maquina.MaquinaKato;
import pe.betaagroindustrial.avance.variedad.Variedad;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Una fila de detalle de un corte: que maquina + kato proceso que
 * variedad, cuantas jabas, y a que peso total (derivado del peso
 * promedio de la variedad segun el ultimo ingreso reportado). El kg por
 * empacador es ese peso total dividido entre los empacadores de esa
 * fila - asi se puede saber cuanto proceso especificamente cada kato de
 * cada maquina, no solo el total agregado del corte.
 */
@Entity
@Table(name = "corte_detalle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorteDetalle extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corte_id", nullable = false)
    private Corte corte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    /** Opcional: los productos sin katos (Vid) registran solo maquina/modulo + variedad. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maquina_kato_id")
    private MaquinaKato maquinaKato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variedad_id", nullable = false)
    private Variedad variedad;

    @Column(name = "jabas", nullable = false, precision = 10, scale = 0)
    @Builder.Default
    private BigDecimal jabas = BigDecimal.ZERO;

    @Column(name = "peso_total", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pesoTotal = BigDecimal.ZERO;

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
