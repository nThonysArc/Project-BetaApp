package pe.betaagroindustrial.avance.proceso.corte;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.betaagroindustrial.avance.common.audit.Auditable;
import pe.betaagroindustrial.avance.proceso.ProcesoDiario;
import pe.betaagroindustrial.avance.usuario.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registro de avance por hora. Es la tabla central del sistema.
 *
 * Los totales (jabasTotalCalculado / pesoTotalCalculado) se derivan siempre
 * de la suma de CorteVariedadDetalle - nunca se escriben "a mano" en esa
 * columna. Si el usuario necesita corregir manualmente (bascula real vs
 * calculo del sistema), se completa el campo *Ajustado correspondiente.
 *
 * El valor que debe usarse en cualquier reporte/KPI es siempre:
 *   valorEfectivo = ajustado != null ? ajustado : calculado
 *
 * clienteUuid es la clave de idempotencia para sincronizacion offline-first:
 * generado en el dispositivo antes de tener conexion, evita duplicados si
 * el POST se reintenta tras una perdida de senal a mitad del envio.
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

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoCorte estado = EstadoCorte.BORRADOR;

    @Column(name = "consolidado_en")
    private OffsetDateTime consolidadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consolidado_por")
    private Usuario consolidadoPor;

    @Column(name = "requiere_revision", nullable = false)
    @Builder.Default
    private boolean requiereRevision = false;

    @Column(name = "cliente_uuid", unique = true)
    private UUID clienteUuid;

    @OneToMany(mappedBy = "corte", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CorteVariedadDetalle> variedades = new ArrayList<>();

    @OneToMany(mappedBy = "corte", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CorteMaquinaKato> maquinasKato = new ArrayList<>();

    /**
     * Valor efectivo de jabas: el ajuste manual tiene siempre prioridad
     * sobre el calculo automatico, sin perder nunca el dato calculado original.
     */
    @Transient
    public BigDecimal getJabasTotalEfectivo() {
        return jabasTotalAjustado != null ? jabasTotalAjustado : jabasTotalCalculado;
    }

    @Transient
    public BigDecimal getPesoTotalEfectivo() {
        return pesoTotalAjustado != null ? pesoTotalAjustado : pesoTotalCalculado;
    }
}
