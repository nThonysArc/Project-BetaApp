package pe.betaagroindustrial.avance.reporte.vid;

/**
 * Hallazgo de consistencia sobre el avance por hora. NO bloquea el guardado:
 * en planta el dato real vale mas que un rechazo, asi que se avisa y se marca
 * en el reporte para que el supervisor lo revise.
 *
 * @param hora   numero de hora (1..n) o null si aplica a todo el dia
 * @param modulo nombre del modulo o null si aplica a toda la hora
 * @param codigo identificador estable (para filtrar en el front y en las pruebas)
 */
public record Advertencia(Integer hora, String modulo, String codigo, String mensaje) {
}
