package cl.duoc.pedidos360.consumer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.pedidos360.config.RabbitMQConstants;

/**
 * =========================================================================
 * CONSUMIDOR DE AUDITORIA: AuditoriaConsumer
 * =========================================================================
 * Escucha la cola de auditoria pedidos.auditoria.queue para registrar la
 * trazabilidad de los eventos de negocio del sistema.
 */
@Component
public class AuditoriaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaConsumer.class);
    private final ObjectMapper json;

    public AuditoriaConsumer(ObjectMapper json) {
        this.json = json;
    }

    @RabbitListener(
            queues = RabbitMQConstants.QUEUE_AUDITORIA,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void registrarAuditoria(
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        JsonNode event;
        try {
            event = json.readTree(message.getBody());
        } catch (IOException invalidJson) {
            log.warn("[AUDITORIA] JSON inválido deliveryTag={}", deliveryTag);
            channel.basicReject(deliveryTag, false);
            return;
        }
        log.info("[AUDITORIA] routingKey={} pedido={}", message.getMessageProperties().getReceivedRoutingKey(),
            event == null ? "" : event.path("numeroPedido").asText());

        channel.basicAck(deliveryTag, false);
    }
}
