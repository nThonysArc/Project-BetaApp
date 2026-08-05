package pe.betaagroindustrial.avance.proceso.corte;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorteRepository extends JpaRepository<Corte, Long> {

    List<Corte> findByProcesoDiarioIdOrderByNumeroCorteAsc(Long procesoDiarioId);

    Optional<Corte> findByClienteUuid(UUID clienteUuid);

    Optional<Corte> findTopByProcesoDiarioIdOrderByNumeroCorteDesc(Long procesoDiarioId);
}
