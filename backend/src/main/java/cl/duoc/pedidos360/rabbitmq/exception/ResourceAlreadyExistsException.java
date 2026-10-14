package cl.duoc.pedidos360.rabbitmq.exception;

/**
 * Excepcion lanzada cuando se intenta crear un recurso que ya existe con configuracion conflictiva
 */
public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
