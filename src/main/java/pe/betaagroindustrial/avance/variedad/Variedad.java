package pe.betaagroindustrial.avance.variedad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.common.audit.Auditable;
import pe.betaagroindustrial.avance.producto.Producto;

/**
 * Catalogo de variedades, scoped por producto (Sekoya Pop, Magica, etc.
 * son variedades de Arandano; otro producto tendria su propio set).
 * Reemplaza el texto libre que antes se escribia en cada corte.
 */
@Entity
@Table(name = "variedad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Variedad extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Short orden = 0;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;
}
