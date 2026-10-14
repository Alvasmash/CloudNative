package cl.duoc.pedidos360.rabbitmq;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.rabbitmq.client.Channel;

import cl.duoc.pedidos360.config.RabbitMQConstants;
import cl.duoc.pedidos360.consumer.PedidoConsumer;
import cl.duoc.pedidos360.event.ItemPedidoEvent;
import cl.duoc.pedidos360.event.PedidoCreadoEvent;

/**
 * =========================================================================
 * PRUEBAS UNITARIAS: PedidoConsumer (ACK/NACK, Reintentos y DLQ)
 * =========================================================================
 * Criterio de Evaluacion EP3 (15%): Consumidores con @RabbitListener.
 * Criterio de Evaluacion EP3 (20%): Politica de ACK/NACK, reintentos con TTL y DLQ.
 * Demuestra:
 * - Procesamiento exitoso y envio de ACK explicito.
 * - Rechazo de eventos invalidos y derivacion a DLQ sin bucles.
 * - Reintento ante error recuperable incrementando cabecera x-retry-count.
 * - Respeto de maximo de intentos y envio definitivo a DLQ tras agotar reintentos.
 */
class PedidoConsumerAckNackAndDlqTest {

    private RabbitTemplate rabbitTemplate;
    private Channel channel;
    private PedidoConsumer consumer;

    @BeforeEach
    void setUp() {
        rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        channel = Mockito.mock(Channel.class);
        consumer = new PedidoConsumer(rabbitTemplate);
    }

    private PedidoCreadoEvent crearEventoValido() {
        return new PedidoCreadoEvent(
                1L,
                "PED-VALID-01",
                "Carlos Perez",
                "carlos@duocuc.cl",
                15990.0,
                "PENDIENTE",
                LocalDateTime.now(),
                List.of(new ItemPedidoEvent(1L, "Pizza Margherita", 2, 7995.0, 15990.0))
        );
    }

    private Message crearMensajeMock(int retryCount) {
        Message message = Mockito.mock(Message.class);
        MessageProperties properties = new MessageProperties();
        properties.setHeader(RabbitMQConstants.HEADER_X_RETRY_COUNT, retryCount);
        when(message.getMessageProperties()).thenReturn(properties);
        return message;
    }

    @Test
    @DisplayName("1. Procesamiento exitoso de pedido envia ACK al broker (deliveryTag 101)")
    void procesarPedido_exitoso_enviaAck() throws IOException {
        PedidoCreadoEvent event = crearEventoValido();
        Message message = crearMensajeMock(0);
        long deliveryTag = 101L;

        consumer.procesarPedido(event, message, channel, deliveryTag);

        // Verifica que se envio basicAck con la deliveryTag correcta
        verify(channel).basicAck(eq(deliveryTag), eq(false));
        // No debe haberse publicado nada en retry ni en DLQ
        verify(rabbitTemplate, never()).convertAndSend(any(), any(), any(), any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("2. Mensaje invalido (sin items ni ID) es rechazado y enviado directamente a DLQ con ACK del original")
    void procesarPedido_invalido_enviaADlqConAck() throws IOException {
        PedidoCreadoEvent eventInvalido = new PedidoCreadoEvent();
        eventInvalido.setId(null); // ID faltante
        eventInvalido.setNumeroPedido("PED-INVALID");
        eventInvalido.setItems(Collections.emptyList()); // Sin items

        Message message = crearMensajeMock(0);
        long deliveryTag = 202L;

        assertFalse(consumer.esMensajeValido(eventInvalido));

        consumer.procesarPedido(eventInvalido, message, channel, deliveryTag);

        // Se envia al DLX hacia la Dead Letter Queue
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE_DLX),
                eq(RabbitMQConstants.RK_DLQ),
                eq(eventInvalido),
                any(MessagePostProcessor.class)
        );

        // Se confirma el mensaje corrupto para retirarlo de la cola y no generar ciclos
        verify(channel).basicAck(eq(deliveryTag), eq(false));
    }

    @Test
    @DisplayName("3. Error recuperable con reintentos pendientes (< 3) publica en cola de reintentos con x-retry-count incrementado")
    void procesarPedido_errorRecuperable_publicaEnRetryQueue() throws IOException {
        PedidoCreadoEvent event = crearEventoValido();
        // Simulacion de fallo en procesamiento
        event.setClienteNombre("FAIL_SIMULATED_TEST");

        Message message = crearMensajeMock(0); // Intento 0
        long deliveryTag = 303L;

        consumer.procesarPedido(event, message, channel, deliveryTag);

        // Debe publicar en el exchange de reintentos con routing key pedido.reintento
        ArgumentCaptor<MessagePostProcessor> postProcessorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE_RETRY),
                eq(RabbitMQConstants.RK_RETRY),
                eq(event),
                postProcessorCaptor.capture()
        );

        // Validar que el header x-retry-count se incremento a 1
        Message m = new Message(new byte[0], new MessageProperties());
        Message resultMessage = postProcessorCaptor.getValue().postProcessMessage(m);
        assertEquals(Integer.valueOf(1), (Integer) resultMessage.getMessageProperties().getHeader(RabbitMQConstants.HEADER_X_RETRY_COUNT));

        // Se confirma el original una vez encolado el reintento
        verify(channel).basicAck(eq(deliveryTag), eq(false));
    }

    @Test
    @DisplayName("4. Maximo de reintentos alcanzado (>= 3) deriva definitivamente a Dead Letter Queue")
    void procesarPedido_maximoReintentos_derivaADlq() throws IOException {
        PedidoCreadoEvent event = crearEventoValido();
        event.setClienteNombre("FAIL_SIMULATED_TEST");

        // Mensaje que ya lleva 3 reintentos fallidos
        Message message = crearMensajeMock(3);
        long deliveryTag = 404L;

        consumer.procesarPedido(event, message, channel, deliveryTag);

        // Debe enviarse al exchange DLX con la routing key de DLQ
        ArgumentCaptor<MessagePostProcessor> dlqCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE_DLX),
                eq(RabbitMQConstants.RK_DLQ),
                eq(event),
                dlqCaptor.capture()
        );

        // Validar que las cabeceras conservan informacion del error
        Message m = new Message(new byte[0], new MessageProperties());
        Message resultMessage = dlqCaptor.getValue().postProcessMessage(m);
        assertEquals(Integer.valueOf(3), (Integer) resultMessage.getMessageProperties().getHeader(RabbitMQConstants.HEADER_X_RETRY_COUNT));
        assertTrue(resultMessage.getMessageProperties().getHeaders().containsKey(RabbitMQConstants.HEADER_X_EXCEPTION_MESSAGE));

        // El original es confirmado para culminar su ciclo en la cola principal
        verify(channel).basicAck(eq(deliveryTag), eq(false));
    }
}
