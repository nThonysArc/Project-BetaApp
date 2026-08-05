package pe.betaagroindustrial.avance.proceso.auditoria;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.usuario.Usuario;

import java.time.OffsetDateTime;

/**
 * Event log generico de ediciones sobre cualquier entidad del dominio de
 * proceso/corte. Una sola tabla sirve para todo el sistema en vez de una
 * tabla de historial por entidad, para poder construir una unica pantalla
 * de "historial de cambios" reutilizable.
 *
 * Se escribe siempre desde el Service correspondiente (nunca desde el
 * Controller) dentro de la misma transaccion que el cambio que audita.
 */
@Entity
@Table(name = "auditoria_edicion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaEdicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entidad", nullable = false, length = 50)
    private String entidad;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(name = "campo", nullable = false, length = 60)
    private String campo;

    @Column(name = "valor_anterior", columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(name = "valor_nuevo", columnDefinition = "TEXT")
    private String valorNuevo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cambio", nullable = false, length = 20)
    private TipoCambio tipoCambio;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    @Builder.Default
    private OffsetDateTime fecha = OffsetDateTime.now();

    public enum TipoCambio {
        EDICION_INPUT,
        AJUSTE_MANUAL,
        RECALCULO
    }
}
