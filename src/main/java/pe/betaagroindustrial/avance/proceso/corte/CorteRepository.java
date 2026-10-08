package pe.betaagroindustrial.avance.proceso.corte;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorteRepository extends JpaRepository<Corte, Long> {

    List<Corte> findByProcesoDiarioIdOrderByNumeroCorteAsc(Long procesoDiarioId);

    Optional<Corte> findByClienteUuid(UUID clienteUuid);

    Optional<Corte> findTopByProcesoDiarioIdOrderByNumeroCorteDesc(Long procesoDiarioId);

    // Corte inmediatamente anterior (para heredar el supervisor de cada maquina al editar).
    Optional<Corte> findTopByProcesoDiarioIdAndNumeroCorteLessThanOrderByNumeroCorteDesc(
            Long procesoDiarioId, Short numeroCorte);

    // Usado para poblar totalCortes en ProcesoDiarioResponse sin cargar los cortes completos.
    int countByProcesoDiarioId(Long procesoDiarioId);
}
