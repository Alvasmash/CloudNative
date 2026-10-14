package cl.duoc.pedidos360.rabbitmq.exception;

/**
 * Excepcion lanzada cuando se intenta eliminar o alterar un recurso critico protegido
 */
public class CriticalResourceException extends RuntimeException {
    public CriticalResourceException(String message) {
        super(message);
    }
}
