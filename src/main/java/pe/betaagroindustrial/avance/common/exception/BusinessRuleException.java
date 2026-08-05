package pe.betaagroindustrial.avance.common.exception;

/**
 * Excepcion para violaciones de reglas de negocio explicitas
 * (ej: intentar consolidar un corte sin motivo, fechas invalidas, etc).
 * Se traduce a HTTP 422 en el GlobalExceptionHandler.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
