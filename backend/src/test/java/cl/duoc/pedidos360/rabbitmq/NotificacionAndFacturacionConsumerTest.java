package cl.duoc.pedidos360.rabbitmq;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.rabbitmq.client.Channel;

import cl.duoc.pedidos360.config.RabbitMQConstants;
import cl.duoc.pedidos360.consumer.FacturacionConsumer;
import cl.duoc.pedidos360.consumer.NotificacionConsumer;
import cl.duoc.pedidos360.event.FacturacionEvent;
import cl.duoc.pedidos360.event.NotificacionEvent;

/**
 * =========================================================================
 * PRUEBAS UNITARIAS: NotificacionConsumer y FacturacionConsumer
 * =========================================================================
 * Valida las Colas 2 y 3 con sus respectivas DLQ, confirmaciones manuales (ACK)
 * y enrutamiento hacia pedidos.dlx.
 */
class NotificacionAndFacturacionConsumerTest {

    private AmqpTemplate amqpTemplate;
    private Channel channel;
    private NotificacionConsumer notificacionConsumer;
    private FacturacionConsumer facturacionConsumer;

    @BeforeEach
    void setUp() {
        amqpTemplate = Mockito.mock(AmqpTemplate.class);
        channel = Mockito.mock(Channel.class);
        notificacionConsumer = new NotificacionConsumer(amqpTemplate);
        facturacionConsumer = new FacturacionConsumer(amqpTemplate);
    }

    @Test
    @DisplayName("1. NotificacionConsumer valida y confirma con ACK un mensaje correcto (Cola 2)")
    void procesarNotificacion_valido_debeEnviarAck() throws IOException {
        NotificacionEvent event = new NotificacionEvent(
                10L, "PED-1234", "Juan Perez", "juan@duocuc.cl",
                "CONFIRMACION", "Tu pedido esta confirmado", LocalDateTime.now()
        );
        Message message = new Message("{}".getBytes(), new MessageProperties());

        notificacionConsumer.procesarNotificacion(event, message, channel, 101L);

        verify(channel).basicAck(101L, false);
        verify(amqpTemplate, Mockito.never()).convertAndSend(any(), any(), any(), any());
    }

    @Test
    @DisplayName("2. NotificacionConsumer deriva a DLQ si correo es invalido (Cola 2 -> DLQ 2)")
    void procesarNotificacion_invalido_debeEnviarADlq() throws IOException {
        NotificacionEvent eventInvalido = new NotificacionEvent(
                10L, "PED-1234", "Juan Perez", "correo-invalido-sin-arroba",
                "CONFIRMACION", "Mensaje", LocalDateTime.now()
        );
        Message message = new Message("{}".getBytes(), new MessageProperties());

        notificacionConsumer.procesarNotificacion(eventInvalido, message, channel, 102L);

        verify(channel).basicAck(102L, false);
        verify(amqpTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE_DLX),
                eq(RabbitMQConstants.RK_NOTIFICACION_DLQ),
                eq(eventInvalido),
                any()
        );
    }

    @Test
    @DisplayName("3. FacturacionConsumer procesa boleta valida y confirma con ACK (Cola 3)")
    void procesarFacturacion_valido_debeEnviarAck() throws IOException {
        FacturacionEvent event = new FacturacionEvent(
                20L, "PED-5678", "DTE-5678", "Maria Lopez", "maria@duocuc.cl",
                10000.0, 1900.0, 11900.0, LocalDateTime.now()
        );
        Message message = new Message("{}".getBytes(), new MessageProperties());

        facturacionConsumer.procesarFacturacion(event, message, channel, 201L);

        verify(channel).basicAck(201L, false);
        verify(amqpTemplate, Mockito.never()).convertAndSend(any(), any(), any(), any());
    }

    @Test
    @DisplayName("4. FacturacionConsumer deriva a DLQ si montos son invalidos (Cola 3 -> DLQ 3)")
    void procesarFacturacion_invalido_debeEnviarADlq() throws IOException {
        FacturacionEvent eventInvalido = new FacturacionEvent(
                20L, "PED-5678", "", "Maria Lopez", "maria@duocuc.cl",
                -500.0, 0.0, -500.0, LocalDateTime.now()
        );
        Message message = new Message("{}".getBytes(), new MessageProperties());

        facturacionConsumer.procesarFacturacion(eventInvalido, message, channel, 202L);

        verify(channel).basicAck(202L, false);
        verify(amqpTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE_DLX),
                eq(RabbitMQConstants.RK_FACTURACION_DLQ),
                eq(eventInvalido),
                any()
        );
    }

    @Test
    @DisplayName("5. Validadores de eventos discriminan correctamente estructuras corruptas")
    void validadores_debenDetectarInconsistencias() {
        assertFalse(notificacionConsumer.esValido(null));
        assertFalse(facturacionConsumer.esValido(null));

        NotificacionEvent okNotif = new NotificacionEvent(1L, "P1", "C1", "c@d.cl", "T", "Msg", null);
        assertTrue(notificacionConsumer.esValido(okNotif));

        FacturacionEvent okFact = new FacturacionEvent(1L, "P1", "FOLIO-1", "C1", "c@d.cl", 100.0, 19.0, 119.0, null);
        assertTrue(facturacionConsumer.esValido(okFact));
    }
}
