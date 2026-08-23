package pe.betaagroindustrial.avance.maquina;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.common.audit.Auditable;

/**
 * Catalogo de katos de una maquina (KATO 1, KATO 2, BULK...), definido
 * una sola vez al crear/editar la maquina. Reemplaza el texto libre que
 * antes se escribia en cada corte, evitando duplicados como "KATO 1"
 * vs "Kato1".
 */
@Entity
@Table(name = "maquina_kato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaquinaKato extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @Column(name = "nombre", nullable = false, length = 30)
    private String nombre;

    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Short orden = 0;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;
}
