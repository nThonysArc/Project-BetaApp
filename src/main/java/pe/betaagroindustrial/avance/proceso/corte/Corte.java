package pe.betaagroindustrial.avance.proceso.corte;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.common.audit.Auditable;
import pe.betaagroindustrial.avance.proceso.ProcesoDiario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registro de avance por hora. Tabla central del sistema.
 *
 * jabasTotalCalculado/pesoTotalCalculado se derivan SIEMPRE de la suma
 * de "detalles" (cada fila maquina+kato+variedad) - nunca se escriben
 * a mano. Para corregir manualmente (bascula real vs calculo del
 * sistema) se usa *Ajustado, que nunca sobrescribe el calculado.
 *
 * valorEfectivo = ajustado != null ? ajustado : calculado
 *
 * No existe un "estado" de consolidacion a nivel de corte individual:
 * el punto de cierre real es el ProcesoDiario completo (ABIERTO/CERRADO).
 * Mientras el proceso este abierto, cualquier corte de esa fecha puede
 * editarse libremente - hasta la ultima hora puede requerir ajustar el
 * peso de la primera, para cuadrar con el ingreso total reportado. Una
 * vez cerrado el proceso, editar cualquiera de sus cortes exige motivo.
 */
@Entity
@Table(name = "corte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Corte extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proceso_diario_id", nullable = false)
    private ProcesoDiario procesoDiario;

    @Column(name = "numero_corte", nullable = false)
    private Short numeroCorte;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "fecha_cosecha")
    private LocalDate fechaCosecha;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "jabas_total_calculado", precision = 10, scale = 0)
    private BigDecimal jabasTotalCalculado;

    @Column(name = "jabas_total_ajustado", precision = 10, scale = 0)
    private BigDecimal jabasTotalAjustado;

    @Column(name = "peso_total_calculado", precision = 12, scale = 2)
    private BigDecimal pesoTotalCalculado;

    @Column(name = "peso_total_ajustado", precision = 12, scale = 2)
    private BigDecimal pesoTotalAjustado;

    @Column(name = "requiere_revision", nullable = false)
    @Builder.Default
    private boolean requiereRevision = false;

    @Column(name = "cliente_uuid", unique = true)
    private UUID clienteUuid;

    @OneToMany(mappedBy = "corte", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CorteDetalle> detalles = new ArrayList<>();

    /** Supervisor de cada maquina en este corte (a lo sumo uno por maquina). */
    @OneToMany(mappedBy = "corte", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CorteMaquina> supervisores = new ArrayList<>();

    @Transient
    public BigDecimal getJabasTotalEfectivo() {
        return jabasTotalAjustado != null ? jabasTotalAjustado : jabasTotalCalculado;
    }

    @Transient
    public BigDecimal getPesoTotalEfectivo() {
        return pesoTotalAjustado != null ? pesoTotalAjustado : pesoTotalCalculado;
    }
}
