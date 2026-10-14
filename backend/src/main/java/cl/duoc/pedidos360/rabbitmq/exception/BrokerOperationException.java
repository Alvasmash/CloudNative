package cl.duoc.pedidos360.rabbitmq.exception;

/**
 * Excepcion lanzada ante errores operativos inesperados en la comunicacion con RabbitMQ
 */
public class BrokerOperationException extends RuntimeException {
    public BrokerOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BrokerOperationException(String message) {
        super(message);
    }
}
