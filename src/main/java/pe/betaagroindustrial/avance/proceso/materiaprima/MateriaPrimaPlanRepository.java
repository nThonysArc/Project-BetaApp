package pe.betaagroindustrial.avance.proceso.materiaprima;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MateriaPrimaPlanRepository extends JpaRepository<MateriaPrimaPlan, Long> {

    List<MateriaPrimaPlan> findByProcesoDiarioIdOrderByOrdenAsc(Long procesoDiarioId);

    Optional<MateriaPrimaPlan> findByProcesoDiarioIdAndVariedadId(Long procesoDiarioId, Long variedadId);
}
