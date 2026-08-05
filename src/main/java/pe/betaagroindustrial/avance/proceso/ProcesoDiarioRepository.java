package pe.betaagroindustrial.avance.proceso;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ProcesoDiarioRepository extends JpaRepository<ProcesoDiario, Long> {

    Optional<ProcesoDiario> findByCampanaIdAndFecha(Long campanaId, LocalDate fecha);
}
