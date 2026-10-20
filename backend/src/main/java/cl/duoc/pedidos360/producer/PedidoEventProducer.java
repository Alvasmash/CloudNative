package cl.duoc.pedidos360.producer;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.config.RabbitMQConstants;
import cl.duoc.pedidos360.event.FacturacionEvent;
import cl.duoc.pedidos360.event.NotificacionEvent;
import cl.duoc.pedidos360.event.PedidoCreadoEvent;

/**
 * =========================================================================
 * PRODUCTOR DE EVENTOS: PedidoEventProducer
 * =========================================================================
 * Criterio de Evaluacion EP3 (5.3): Publica eventos de negocio hacia RabbitMQ
 * utilizando DTOs especificos desacoplados del modelo JPA.
 * Agrega headers de trazabilidad iniciales (x-retry-count: 0, timestamp, etc.).
 */
@Component
public class PedidoEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public PedidoEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica el evento de pedido creado en el exchange de pedidos con routing key pedido.creado.
     * Incorpora cabeceras de trazabilidad iniciales.
     */
    public void publicarPedidoCreado(PedidoCreadoEvent event) {
        if (event == null) {
            log.warn("Intento de publicar evento nulo de pedido. Operacion omitida.");
            return;
        }

        try {
            log.info("Publicando evento PedidoCreadoEvent para pedido [numero: {}, total: ${}] al exchange '{}' con routing key '{}'",
                    event.getNumeroPedido(), event.getTotal(), RabbitMQConstants.EXCHANGE_PEDIDOS, RabbitMQConstants.RK_PEDIDO_CREADO);

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_PEDIDOS,
                    RabbitMQConstants.RK_PEDIDO_CREADO,
                    event,
                    message -> {
                        var props = message.getMessageProperties();
                        props.setHeader(RabbitMQConstants.HEADER_X_RETRY_COUNT, 0);
                        props.setHeader(RabbitMQConstants.HEADER_X_MAX_RETRIES, RabbitMQConstants.DEFAULT_MAX_RETRIES);
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_EXCHANGE, RabbitMQConstants.EXCHANGE_PEDIDOS);
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_ROUTING_KEY, RabbitMQConstants.RK_PEDIDO_CREADO);
                        props.setHeader(RabbitMQConstants.HEADER_X_TIMESTAMP, Instant.now().toString());
                        return message;
                    }
            );

            log.info("Evento publicado exitosamente para pedido {}", event.getNumeroPedido());
        } catch (Exception e) {
            log.error("Error critico al publicar evento de pedido {} en RabbitMQ: {}",
                    event.getNumeroPedido(), e.getMessage(), e);
            throw new RuntimeException("Fallo al publicar evento de pedido en RabbitMQ: " + e.getMessage(), e);
        }
    }

    /**
     * Publica el evento de notificacion para el cliente en el exchange de pedidos con routing key pedido.notificacion.
     */
    public void publicarNotificacion(NotificacionEvent event) {
        if (event == null) {
            log.warn("Intento de publicar evento nulo de notificacion. Operacion omitida.");
            return;
        }

        try {
            log.info("Publicando NotificacionEvent para cliente {} [pedido: {}] a exchange '{}' con routing key '{}'",
                    event.getClienteEmail(), event.getNumeroPedido(),
                    RabbitMQConstants.EXCHANGE_PEDIDOS, RabbitMQConstants.RK_NOTIFICACION);

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_PEDIDOS,
                    RabbitMQConstants.RK_NOTIFICACION,
                    event,
                    message -> {
                        var props = message.getMessageProperties();
                        props.setHeader(RabbitMQConstants.HEADER_X_RETRY_COUNT, 0);
                        props.setHeader(RabbitMQConstants.HEADER_X_MAX_RETRIES, RabbitMQConstants.DEFAULT_MAX_RETRIES);
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_EXCHANGE, RabbitMQConstants.EXCHANGE_PEDIDOS);
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_ROUTING_KEY, RabbitMQConstants.RK_NOTIFICACION);
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_QUEUE, RabbitMQConstants.QUEUE_NOTIFICACION);
                        props.setHeader(RabbitMQConstants.HEADER_X_TIMESTAMP, Instant.now().toString());
                        return message;
                    }
            );

            log.info("NotificacionEvent publicado exitosamente para pedido {}", event.getNumeroPedido());
        } catch (Exception e) {
            log.error("Error al publicar evento de notificacion para pedido {}: {}",
                    event.getNumeroPedido(), e.getMessage(), e);
            throw new RuntimeException("Fallo al publicar evento de notificacion en RabbitMQ: " + e.getMessage(), e);
        }
    }

    /**
     * Publica el evento de facturacion en el exchange de pedidos con routing key pedido.facturacion.
     */
    public void publicarFacturacion(FacturacionEvent event) {
        if (event == null) {
            log.warn("Intento de publicar evento nulo de facturacion. Operacion omitida.");
            return;
        }

        try {
            log.info("Publicando FacturacionEvent [folio: {}, total: ${}] a exchange '{}' con routing key '{}'",
                    event.getFolioFiscal(), event.getMontoTotal(),
                    RabbitMQConstants.EXCHANGE_PEDIDOS, RabbitMQConstants.RK_FACTURACION);

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_PEDIDOS,
                    RabbitMQConstants.RK_FACTURACION,
                    event,
                    message -> {
                        var props = message.getMessageProperties();
                        props.setHeader(RabbitMQConstants.HEADER_X_RETRY_COUNT, 0);
                        props.setHeader(RabbitMQConstants.HEADER_X_MAX_RETRIES, RabbitMQConstants.DEFAULT_MAX_RETRIES);
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_EXCHANGE, RabbitMQConstants.EXCHANGE_PEDIDOS);
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_ROUTING_KEY, RabbitMQConstants.RK_FACTURACION);
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_QUEUE, RabbitMQConstants.QUEUE_FACTURACION);
                        props.setHeader(RabbitMQConstants.HEADER_X_TIMESTAMP, Instant.now().toString());
                        return message;
                    }
            );

            log.info("FacturacionEvent publicado exitosamente para pedido {}", event.getNumeroPedido());
        } catch (Exception e) {
            log.error("Error al publicar evento de facturacion para pedido {}: {}",
                    event.getNumeroPedido(), e.getMessage(), e);
            throw new RuntimeException("Fallo al publicar evento de facturacion en RabbitMQ: " + e.getMessage(), e);
        }
    }

    /**
     * Publica los 3 eventos asincronos del ciclo de negocio:
     * 1. Preparacion en cocina (Cola 1)
     * 2. Notificacion al cliente (Cola 2)
     * 3. Boleta fiscal DTE (Cola 3)
     */
    public void publicarCicloCompleto(PedidoCreadoEvent pedidoEvent, NotificacionEvent notifEvent, FacturacionEvent factEvent) {
        publicarPedidoCreado(pedidoEvent);
        if (notifEvent != null) {
            publicarNotificacion(notifEvent);
        }
        if (factEvent != null) {
            publicarFacturacion(factEvent);
        }
    }
}
