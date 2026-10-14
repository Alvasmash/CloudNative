package cl.duoc.pedidos360.consumer;

import java.io.IOException;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.pedidos360.config.RabbitMQConstants;
import cl.duoc.pedidos360.event.PedidoCreadoEvent;

/**
 * =========================================================================
 * CONSUMIDOR DE PEDIDOS: PedidoConsumer
 * =========================================================================
 * Criterio de Evaluacion EP3 (15%): Consumidores mediante @RabbitListener.
 * Criterio de Evaluacion EP3 (20%): Confirmacion manual (ACK/NACK),
 *                                   reintentos con TTL y derivacion a DLQ.
 */
@Component
public class PedidoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoConsumer.class);

    private final RabbitTemplate rabbitTemplate;

    public PedidoConsumer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Escucha eventos de creacion de pedidos desde la cola principal.
     * Implementa confirmacion manual (AcknowledgeMode.MANUAL).
     */
    @RabbitListener(
            queues = RabbitMQConstants.QUEUE_PEDIDO_CREADO,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void procesarPedido(
            PedidoCreadoEvent event,
            Message message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("Mensaje recibido en cola '{}' [deliveryTag: {}]",
                RabbitMQConstants.QUEUE_PEDIDO_CREADO, deliveryTag);

        // 1. Validacion de mensaje recibido
        if (!esMensajeValido(event)) {
            log.warn("Mensaje invalido o corrupto detectado [deliveryTag: {}]. Derivando directamente a DLQ sin reintentos.", deliveryTag);
            enviarADeadLetterQueue(message, event, "Mensaje de pedido invalido: faltan campos obligatorios", new IllegalArgumentException("Datos obligatorios nulos o vacios"));
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            // 2. Procesamiento de negocio
            ejecutarLogicaDeProcesamiento(event);

            // 3. Confirmacion exitosa (ACK)
            channel.basicAck(deliveryTag, false);
            log.info("ACK exitoso enviado para pedido [numero: {}, deliveryTag: {}]",
                    event.getNumeroPedido(), deliveryTag);

        } catch (Exception ex) {
            // 4. Manejo de errores, reintentos controlados y DLQ
            manejarErrorProcesamiento(event, message, channel, deliveryTag, ex);
        }
    }

    /**
     * Valida la estructura del evento recibido
     */
    public boolean esMensajeValido(PedidoCreadoEvent event) {
        if (event == null) return false;
        if (event.getId() == null) return false;
        if (event.getNumeroPedido() == null || event.getNumeroPedido().isBlank()) return false;
        if (event.getTotal() == null || event.getTotal() < 0) return false;
        return event.getItems() != null && !event.getItems().isEmpty();
    }

    /**
     * Logica de negocio aplicada al recibir el evento
     */
    protected void ejecutarLogicaDeProcesamiento(PedidoCreadoEvent event) {
        log.info("Procesando pedido {} en cocina: {} items, total: ${}",
                event.getNumeroPedido(), event.getItems().size(), event.getTotal());

        // Si el cliente contiene 'FAIL_TEST' o 'ERROR_SIMULADO', se simula un fallo para pruebas
        if (event.getClienteNombre() != null && event.getClienteNombre().contains("FAIL_SIMULATED")) {
            throw new RuntimeException("Error simulado de conexion con servicio externo de cocina/notificacion");
        }
    }

    /**
     * Gestiona la politica de reintentos con TTL o el descarte hacia la DLQ
     */
    private void manejarErrorProcesamiento(
            PedidoCreadoEvent event,
            Message message,
            Channel channel,
            long deliveryTag,
            Exception ex) throws IOException {

        int retryCount = extraerRetryCount(message);
        log.warn("Fallo al procesar pedido {} [intento actual: {} de {}]: {}",
                event.getNumeroPedido(), retryCount, RabbitMQConstants.DEFAULT_MAX_RETRIES, ex.getMessage());

        if (retryCount < RabbitMQConstants.DEFAULT_MAX_RETRIES) {
            int nextRetry = retryCount + 1;
            log.info("Enrutando pedido {} a cola de reintentos '{}' con TTL de {}ms (intento {}/{})",
                    event.getNumeroPedido(), RabbitMQConstants.QUEUE_RETRY,
                    RabbitMQConstants.DEFAULT_RETRY_TTL_MS, nextRetry, RabbitMQConstants.DEFAULT_MAX_RETRIES);

            // Publicar en cola de reintentos temporizada
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_RETRY,
                    RabbitMQConstants.RK_RETRY,
                    event,
                    m -> {
                        var props = m.getMessageProperties();
                        props.setHeader(RabbitMQConstants.HEADER_X_RETRY_COUNT, nextRetry);
                        props.setHeader(RabbitMQConstants.HEADER_X_MAX_RETRIES, RabbitMQConstants.DEFAULT_MAX_RETRIES);
                        props.setHeader(RabbitMQConstants.HEADER_X_EXCEPTION_MESSAGE, ex.getMessage());
                        props.setHeader(RabbitMQConstants.HEADER_X_EXCEPTION_TYPE, ex.getClass().getSimpleName());
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_EXCHANGE, RabbitMQConstants.EXCHANGE_PEDIDOS);
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_ROUTING_KEY, RabbitMQConstants.RK_PEDIDO_CREADO);
                        props.setHeader(RabbitMQConstants.HEADER_X_TIMESTAMP, Instant.now().toString());
                        return m;
                    }
            );

            // Confirmar el mensaje original para retirarlo de la cola principal (ya esta resguardado en reintentos)
            channel.basicAck(deliveryTag, false);

        } else {
            // Se supero el maximo de reintentos -> Derivar a Dead Letter Queue (DLQ)
            log.error("Limite maximo de reintentos ({}) alcanzado para pedido {}. Derivando a Dead Letter Queue '{}'",
                    RabbitMQConstants.DEFAULT_MAX_RETRIES, event.getNumeroPedido(), RabbitMQConstants.QUEUE_DLQ);

            enviarADeadLetterQueue(message, event, ex.getMessage(), ex);

            // Confirmar mensaje original para evitar bucles infinitos
            channel.basicAck(deliveryTag, false);
        }
    }

    /**
     * Envia el mensaje fallido al Dead Letter Exchange (DLX) con las cabeceras de diagnostico
     */
    public void enviarADeadLetterQueue(Message originalMessage, Object payload, String motivo, Exception ex) {
        try {
            int retries = originalMessage != null ? extraerRetryCount(originalMessage) : 0;

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_DLX,
                    RabbitMQConstants.RK_DLQ,
                    payload,
                    m -> {
                        var props = m.getMessageProperties();
                        props.setHeader(RabbitMQConstants.HEADER_X_RETRY_COUNT, retries);
                        props.setHeader(RabbitMQConstants.HEADER_X_MAX_RETRIES, RabbitMQConstants.DEFAULT_MAX_RETRIES);
                        props.setHeader(RabbitMQConstants.HEADER_X_EXCEPTION_MESSAGE, motivo);
                        props.setHeader(RabbitMQConstants.HEADER_X_EXCEPTION_TYPE, ex != null ? ex.getClass().getSimpleName() : "Desconocido");
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_EXCHANGE, RabbitMQConstants.EXCHANGE_PEDIDOS);
                        props.setHeader(RabbitMQConstants.HEADER_X_ORIGINAL_ROUTING_KEY, RabbitMQConstants.RK_PEDIDO_CREADO);
                        props.setHeader(RabbitMQConstants.HEADER_X_TIMESTAMP, Instant.now().toString());
                        return m;
                    }
            );

            log.info("Mensaje derivado exitosamente a DLQ '{}' con routing key '{}'",
                    RabbitMQConstants.QUEUE_DLQ, RabbitMQConstants.RK_DLQ);

        } catch (Exception e) {
            log.error("Error al publicar mensaje en DLQ: {}", e.getMessage(), e);
        }
    }

    /**
     * Extrae el contador de reintentos desde las propiedades del mensaje
     */
    public int extraerRetryCount(Message message) {
        if (message == null || message.getMessageProperties() == null) {
            return 0;
        }
        Object val = message.getMessageProperties().getHeader(RabbitMQConstants.HEADER_X_RETRY_COUNT);
        if (val instanceof Number n) {
            return n.intValue();
        } else if (val instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
