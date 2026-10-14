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

import cl.duoc.pedidos360.config.RabbitMQConstants;
import cl.duoc.pedidos360.event.PedidoCreadoEvent;

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

    @RabbitListener(
            queues = RabbitMQConstants.QUEUE_AUDITORIA,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void registrarAuditoria(
            PedidoCreadoEvent event,
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("[AUDITORIA] Evento capturado: Pedido {} por cliente '{}' - Total: ${}",
                event != null ? event.getNumeroPedido() : "N/A",
                event != null ? event.getClienteNombre() : "N/A",
                event != null ? event.getTotal() : 0);

        channel.basicAck(deliveryTag, false);
    }
}
