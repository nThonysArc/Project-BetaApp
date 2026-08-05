package pe.betaagroindustrial.avance.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * Superclase mapeada para columnas de auditoria comunes.
 * created_at/updated_at se completan automaticamente via Hibernate Envers-like
 * listener (AuditingEntityListener), y created_by/updated_by via AuditorAware,
 * tomando el usuario autenticado del contexto de seguridad.
 *
 * Los triggers de PostgreSQL (set_updated_at) son una segunda capa de defensa
 * para updated_at, por si algun dato se inserta/actualiza fuera de JPA
 * (por ejemplo, scripts de migracion de datos).
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;
}
