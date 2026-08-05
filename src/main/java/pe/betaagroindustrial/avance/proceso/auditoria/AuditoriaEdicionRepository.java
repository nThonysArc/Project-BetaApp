package pe.betaagroindustrial.avance.proceso.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaEdicionRepository extends JpaRepository<AuditoriaEdicion, Long> {

    List<AuditoriaEdicion> findByEntidadAndEntidadIdOrderByFechaDesc(String entidad, Long entidadId);
}
