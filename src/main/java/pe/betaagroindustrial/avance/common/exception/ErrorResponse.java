package pe.betaagroindustrial.avance.common.exception;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Estructura uniforme de error para toda la API.
 * Se serializa igual sin importar el tipo de excepcion capturada.
 */
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<CampoError> errores
) {
    public record CampoError(String campo, String mensaje) {
    }
}
