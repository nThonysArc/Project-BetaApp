package pe.betaagroindustrial.avance.proceso.corte;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    /** Jabas (efectivas) procesadas en los dias anteriores de una campana; null si no hay ninguna. */
    @Query("select sum(coalesce(c.jabasTotalAjustado, c.jabasTotalCalculado)) from Corte c "
            + "where c.procesoDiario.campana.id = :campanaId and c.procesoDiario.fecha < :fecha")
    BigDecimal sumarJabasAnteriores(@Param("campanaId") Long campanaId, @Param("fecha") LocalDate fecha);

    // Usado para poblar totalCortes en ProcesoDiarioResponse sin cargar los cortes completos.
    int countByProcesoDiarioId(Long procesoDiarioId);
}
