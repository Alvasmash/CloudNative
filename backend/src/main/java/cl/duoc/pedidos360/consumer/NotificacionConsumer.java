package cl.duoc.pedidos360.consumer;

import java.io.IOException;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.pedidos360.config.RabbitMQConstants;
import cl.duoc.pedidos360.event.NotificacionEvent;

/**
 * =========================================================================
 * CONSUMIDOR DE NOTIFICACIONES: NotificacionConsumer
 * =========================================================================
 * Dominio: Notificaciones asincronas a clientes (Correo / SMS / Push).
 * Criterio EP3: Consumidor por dominio funcional con confirmacion manual (ACK),
 * manejo de excepciones y derivacion a DLQ (pedidos.notificacion.dlq).
 */
@Component
public class NotificacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacionConsumer.class);

    private final AmqpTemplate rabbitTemplate;

    public NotificacionConsumer(AmqpTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(
            queues = RabbitMQConstants.QUEUE_NOTIFICACION,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void procesarNotificacion(
            NotificacionEvent event,
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("Mensaje recibido en cola '{}' [deliveryTag: {}] para destinatario: {}",
                RabbitMQConstants.QUEUE_NOTIFICACION, deliveryTag, event != null ? event.getClienteEmail() : "null");

        // 1. Validar integridad del evento
        if (!esValido(event)) {
            log.warn("Notificacion invalida detectada [deliveryTag: {}]. Derivando a DLQ: {}",
                    deliveryTag, RabbitMQConstants.QUEUE_NOTIFICACION_DLQ);
            enviarADeadLetterQueue(message, event, "Datos de notificacion incompletos o correo invalido",
                    new IllegalArgumentException("Email o mensaje nulo/vacio"));
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            // 2. Procesamiento de negocio (simulacion de envio de correo/SMS)
            ejecutarEnvioNotificacion(event);

            // 3. Confirmacion manual exitosa (ACK)
            channel.basicAck(deliveryTag, false);
            log.info("Notificacion enviada y confirmada con ACK [deliveryTag: {}] para pedido {}",
                    deliveryTag, event.getNumeroPedido());

        } catch (Exception ex) {
            log.error("Fallo durante el envio de notificacion para pedido {}: {}",
                    event.getNumeroPedido(), ex.getMessage(), ex);

            enviarADeadLetterQueue(message, event, "Error en pasarela de notificaciones: " + ex.getMessage(), ex);
            channel.basicAck(deliveryTag, false);
        }
    }

    private void ejecutarEnvioNotificacion(NotificacionEvent event) {
        log.info("[NOTIFICACION-SERVICE] -> Enviando mensaje '{}' al correo '{}' [Tipo: {}]",
                event.getMensaje(), event.getClienteEmail(), event.getTipoNotificacion());
    }

    public boolean esValido(NotificacionEvent event) {
        return event != null
                && event.getClienteEmail() != null
                && !event.getClienteEmail().isBlank()
                && event.getClienteEmail().contains("@")
                && event.getMensaje() != null
                && !event.getMensaje().isBlank();
    }

    public void enviarADeadLetterQueue(Message originalMessage, Object payload, String motivo, Exception ex) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_DLX,
                    RabbitMQConstants.RK_NOTIFICACION_DLQ,
                    payload,
                    m -> {
                        var props = m.getMessageProperties();
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_QUEUE, RabbitMQConstants.QUEUE_NOTIFICACION);
                        props.setHeader(RabbitMQConstants.HEADER_X_EXCEPTION_MESSAGE, motivo);
                        props.setHeader(RabbitMQConstants.HEADER_X_EXCEPTION_TYPE, ex != null ? ex.getClass().getSimpleName() : "Desconocido");
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_EXCHANGE, RabbitMQConstants.EXCHANGE_PEDIDOS);
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_ROUTING_KEY, RabbitMQConstants.RK_NOTIFICACION);
                        props.setHeader(RabbitMQConstants.HEADER_X_TIMESTAMP, Instant.now().toString());
                        return m;
                    }
            );

            log.info("Notificacion derivada exitosamente a DLQ '{}' con routing key '{}'",
                    RabbitMQConstants.QUEUE_NOTIFICACION_DLQ, RabbitMQConstants.RK_NOTIFICACION_DLQ);
        } catch (Exception e) {
            log.error("Error al derivar notificacion a DLQ: {}", e.getMessage(), e);
        }
    }
}
