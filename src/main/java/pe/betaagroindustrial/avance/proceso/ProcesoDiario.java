package pe.betaagroindustrial.avance.proceso;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.campana.Campana;
import pe.betaagroindustrial.avance.common.audit.Auditable;

import java.time.LocalDate;

@Entity
@Table(name = "proceso_diario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcesoDiario extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campana_id", nullable = false)
    private Campana campana;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Builder.Default
    private EstadoProceso estado = EstadoProceso.ABIERTO;

    /** Jabas que se proyecta procesar en el dia (plantilla Vid). null = sin proyeccion registrada. */
    @Column(name = "proyeccion_jabas", precision = 10, scale = 0)
    private java.math.BigDecimal proyeccionJabas;

    /** Jabas que quedaron de dias previos (stock digitado a mano). null = sin dato: el reporte lo deduce del historial. */
    @Column(name = "stock_anterior_jabas", precision = 10, scale = 0)
    private java.math.BigDecimal stockAnteriorJabas;

    /** Dia al que corresponde ese stock (por defecto, el dia anterior al proceso). */
    @Column(name = "stock_anterior_fecha")
    private LocalDate stockAnteriorFecha;
}
