package pe.betaagroindustrial.avance.campana;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.common.audit.Auditable;
import pe.betaagroindustrial.avance.producto.Producto;

import java.time.LocalDate;

@Entity
@Table(name = "campana")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campana extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "anio", nullable = false)
    private Short anio;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "activa", nullable = false)
    @Builder.Default
    private boolean activa = true;
}
