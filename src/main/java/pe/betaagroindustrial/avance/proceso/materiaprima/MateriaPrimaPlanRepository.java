package pe.betaagroindustrial.avance.proceso.materiaprima;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MateriaPrimaPlanRepository extends JpaRepository<MateriaPrimaPlan, Long> {

    List<MateriaPrimaPlan> findByProcesoDiarioIdOrderByOrdenAsc(Long procesoDiarioId);
}
