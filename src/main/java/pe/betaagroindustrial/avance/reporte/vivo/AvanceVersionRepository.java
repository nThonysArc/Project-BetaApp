package pe.betaagroindustrial.avance.reporte.vivo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Consultas agregadas (count + max de fecha de modificacion) que resumen "el estado" de un proceso diario
 * sin cargar ninguna fila. Cuestan lo mismo con 1 corte que con 40, por eso la web puede preguntarlas
 * cada pocos segundos.
 *
 * Se cuenta ademas de tomar el maximo porque borrar una fila (p. ej. un detalle) no sube ningun maximo,
 * pero si cambia el conteo.
 */
@Repository
public class AvanceVersionRepository {

    @PersistenceContext
    private EntityManager em;

    public record Estadistica(long cantidad, Instant ultimaModificacion) {
    }

    public Estadistica proceso(Long procesoDiarioId) {
        return una("select count(p), max(coalesce(p.updatedAt, p.createdAt)) from ProcesoDiario p where p.id = :id",
                procesoDiarioId);
    }

    public Estadistica cortes(Long procesoDiarioId) {
        return una("select count(c), max(coalesce(c.updatedAt, c.createdAt)) from Corte c "
                + "where c.procesoDiario.id = :id", procesoDiarioId);
    }

    public Estadistica detalles(Long procesoDiarioId) {
        return una("select count(d), max(coalesce(d.updatedAt, d.createdAt)) from CorteDetalle d "
                + "where d.corte.procesoDiario.id = :id", procesoDiarioId);
    }

    public Estadistica supervisores(Long procesoDiarioId) {
        return una("select count(m), max(coalesce(m.updatedAt, m.createdAt)) from CorteMaquina m "
                + "where m.corte.procesoDiario.id = :id", procesoDiarioId);
    }

    public Estadistica materiaPrima(Long procesoDiarioId) {
        return una("select count(m), max(coalesce(m.updatedAt, m.createdAt)) from MateriaPrimaPlan m "
                + "where m.procesoDiario.id = :id", procesoDiarioId);
    }

    private Estadistica una(String jpql, Long id) {
        Object[] fila = em.createQuery(jpql, Object[].class).setParameter("id", id).getSingleResult();
        return new Estadistica(((Number) fila[0]).longValue(), (Instant) fila[1]);
    }
}
