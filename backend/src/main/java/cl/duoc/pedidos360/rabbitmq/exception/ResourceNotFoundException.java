package cl.duoc.pedidos360.rabbitmq.exception;

/**
 * Excepcion lanzada cuando una cola, exchange o binding solicitado no existe
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
