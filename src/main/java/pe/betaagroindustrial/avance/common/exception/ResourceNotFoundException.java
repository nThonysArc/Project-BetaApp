package pe.betaagroindustrial.avance.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entidad, Object id) {
        return new ResourceNotFoundException("%s no encontrado con id: %s".formatted(entidad, id));
    }
}
