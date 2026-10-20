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
import cl.duoc.pedidos360.event.FacturacionEvent;

/**
 * =========================================================================
 * CONSUMIDOR DE FACTURACION: FacturacionConsumer
 * =========================================================================
 * Dominio: Emision de Documentos Tributarios Electronicos (Boletas DTE).
 * Criterio EP3: Consumidor por dominio funcional con confirmacion manual (ACK),
 * calculo fiscal, manejo de excepciones y derivacion a DLQ (pedidos.facturacion.dlq).
 */
@Component
public class FacturacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(FacturacionConsumer.class);

    private final AmqpTemplate rabbitTemplate;

    public FacturacionConsumer(AmqpTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(
            queues = RabbitMQConstants.QUEUE_FACTURACION,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void procesarFacturacion(
            FacturacionEvent event,
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("Mensaje recibido en cola '{}' [deliveryTag: {}] para folio fiscal: {}",
                RabbitMQConstants.QUEUE_FACTURACION, deliveryTag, event != null ? event.getFolioFiscal() : "null");

        // 1. Validacion del evento fiscal
        if (!esValido(event)) {
            log.warn("Evento de facturacion invalido detectado [deliveryTag: {}]. Derivando a DLQ: {}",
                    deliveryTag, RabbitMQConstants.QUEUE_FACTURACION_DLQ);
            enviarADeadLetterQueue(message, event, "Datos tributarios invalidos (monto <= 0 o folio ausente)",
                    new IllegalArgumentException("Montos o folios tributarios invalidos"));
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            // 2. Procesamiento de emision fiscal (calculo y registro de DTE)
            emitirDocumentoTributario(event);

            // 3. Confirmacion manual exitosa (ACK)
            channel.basicAck(deliveryTag, false);
            log.info("Documento fiscal {} emitido y confirmado con ACK [deliveryTag: {}]",
                    event.getFolioFiscal(), deliveryTag);

        } catch (Exception ex) {
            log.error("Fallo durante emision de documento fiscal para pedido {}: {}",
                    event.getNumeroPedido(), ex.getMessage(), ex);

            enviarADeadLetterQueue(message, event, "Error en servicio de impuestos/facturacion: " + ex.getMessage(), ex);
            channel.basicAck(deliveryTag, false);
        }
    }

    private void emitirDocumentoTributario(FacturacionEvent event) {
        log.info("[FACTURACION-SERVICE] -> Generando DTE Boleta Electronica: Folio={}, Cliente={}, Neto=${}, IVA(19%)=${}, Total=${}",
                event.getFolioFiscal(), event.getClienteNombre(), event.getMontoNeto(), event.getMontoIva(), event.getMontoTotal());
    }

    public boolean esValido(FacturacionEvent event) {
        return event != null
                && event.getFolioFiscal() != null
                && !event.getFolioFiscal().isBlank()
                && event.getMontoTotal() != null
                && event.getMontoTotal() > 0
                && event.getMontoNeto() != null
                && event.getMontoNeto() > 0;
    }

    public void enviarADeadLetterQueue(Message originalMessage, Object payload, String motivo, Exception ex) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_DLX,
                    RabbitMQConstants.RK_FACTURACION_DLQ,
                    payload,
                    m -> {
                        var props = m.getMessageProperties();
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_QUEUE, RabbitMQConstants.QUEUE_FACTURACION);
                        props.setHeader(RabbitMQConstants.HEADER_X_EXCEPTION_MESSAGE, motivo);
                        props.setHeader(RabbitMQConstants.HEADER_X_EXCEPTION_TYPE, ex != null ? ex.getClass().getSimpleName() : "Desconocido");
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_EXCHANGE, RabbitMQConstants.EXCHANGE_PEDIDOS);
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_ROUTING_KEY, RabbitMQConstants.RK_FACTURACION);
                        props.setHeader(RabbitMQConstants.HEADER_X_TIMESTAMP, Instant.now().toString());
                        return m;
                    }
            );

            log.info("Facturacion derivada exitosamente a DLQ '{}' con routing key '{}'",
                    RabbitMQConstants.QUEUE_FACTURACION_DLQ, RabbitMQConstants.RK_FACTURACION_DLQ);
        } catch (Exception e) {
            log.error("Error al derivar facturacion a DLQ: {}", e.getMessage(), e);
        }
    }
}
