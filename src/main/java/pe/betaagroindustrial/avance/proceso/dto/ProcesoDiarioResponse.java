package pe.betaagroindustrial.avance.proceso.dto;

import java.time.LocalDate;

/**
 * totalCortes se incluye aqui para que el historial frontend pueda mostrar
 * cuantos cortes tiene cada dia sin necesitar una llamada extra por proceso.
 * Patron: el response lleva el dato de agregacion minima que el listado necesita
 * para ser util (evita N+1 de llamadas en la pantalla de historial).
 */
public record ProcesoDiarioResponse(
        Long id,
        Long campanaId,
        String campanaNombre,
        LocalDate fecha,
        String estado,
        int totalCortes
) {
}
