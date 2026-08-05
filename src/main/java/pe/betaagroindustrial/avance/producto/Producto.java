package pe.betaagroindustrial.avance.producto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.common.audit.Auditable;

/**
 * Catalogo de productos (Arandano, Esparrago, Uva, Granada...).
 * Discriminador del esquema multi-producto: agregar un producto nuevo
 * no requiere cambios de estructura, solo un registro nuevo aqui.
 */
@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "unidad_medida", nullable = false, length = 20)
    @Builder.Default
    private String unidadMedida = "KG";

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;
}
