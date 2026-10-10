package pe.betaagroindustrial.avance.proceso.corte;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.common.audit.Auditable;
import pe.betaagroindustrial.avance.maquina.Maquina;
import pe.betaagroindustrial.avance.usuario.Usuario;

/**
 * Quien superviso una maquina durante un corte (una hora) especifico.
 *
 * Existe a lo sumo UNA fila por (corte, maquina): el supervisor puede
 * rotar entre maquinas a lo largo del dia, pero dentro de una misma
 * hora una maquina tiene un solo supervisor (restriccion UNIQUE en BD).
 *
 * supervisor puede ser null: la maquina estuvo sin supervisor asignado.
 *
 * observacion: nota de ESA maquina en ESA hora (aparece en la columna
 * OBSERVACION del reporte); puede tener varias lineas.
 */
@Entity
@Table(name = "corte_maquina")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorteMaquina extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "corte_id", nullable = false)
    private Corte corte;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Usuario supervisor;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    /** La maquina salio a almorzar en esta hora: su produccion es 0 y el reporte lo indica. */
    @Column(name = "almuerzo", nullable = false)
    @Builder.Default
    private boolean almuerzo = false;
}
