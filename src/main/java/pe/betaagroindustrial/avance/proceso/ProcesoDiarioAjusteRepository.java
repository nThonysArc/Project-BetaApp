package pe.betaagroindustrial.avance.proceso;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcesoDiarioAjusteRepository extends JpaRepository<ProcesoDiarioAjuste, Long> {

    Optional<ProcesoDiarioAjuste> findTopByProcesoDiarioIdOrderByAjustadoEnDesc(Long procesoDiarioId);
}
