package pe.betaagroindustrial.avance.proceso.materiaprima;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MateriaPrimaPlanRepository extends JpaRepository<MateriaPrimaPlan, Long> {

    List<MateriaPrimaPlan> findByProcesoDiarioIdOrderByOrdenAsc(Long procesoDiarioId);

    Optional<MateriaPrimaPlan> findByProcesoDiarioIdAndVariedadId(Long procesoDiarioId, Long variedadId);

    /** Jabas ingresadas en los dias anteriores de una campana; null si no hay ninguna. */
    @Query("select sum(m.jabasPlan) from MateriaPrimaPlan m "
            + "where m.procesoDiario.campana.id = :campanaId and m.procesoDiario.fecha < :fecha")
    BigDecimal sumarJabasAnteriores(@Param("campanaId") Long campanaId, @Param("fecha") LocalDate fecha);
}
