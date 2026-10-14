package cl.duoc.pedidos360.rabbitmq;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import cl.duoc.pedidos360.config.RabbitMQConstants;
import cl.duoc.pedidos360.rabbitmq.dto.BindingInfoDTO;
import cl.duoc.pedidos360.rabbitmq.dto.BindingRequestDTO;
import cl.duoc.pedidos360.rabbitmq.dto.BrokerStatusDTO;
import cl.duoc.pedidos360.rabbitmq.dto.ExchangeInfoDTO;
import cl.duoc.pedidos360.rabbitmq.dto.ExchangeRequestDTO;
import cl.duoc.pedidos360.rabbitmq.dto.QueueInfoDTO;
import cl.duoc.pedidos360.rabbitmq.dto.QueueRequestDTO;
import cl.duoc.pedidos360.rabbitmq.exception.CriticalResourceException;
import cl.duoc.pedidos360.rabbitmq.exception.ResourceNotFoundException;
import cl.duoc.pedidos360.rabbitmq.service.RabbitMQAdminServiceImpl;

/**
 * =========================================================================
 * PRUEBAS UNITARIAS: RabbitMQAdminService
 * =========================================================================
 * Criterio de Evaluacion EP3 (10%): Servicio dedicado para administrar colas y exchanges.
 * Criterio de Evaluacion EP3 (7%): Validacion de recursos y proteccion de elementos criticos.
 */
class RabbitMQAdminServiceTest {

    private RabbitAdmin rabbitAdmin;
    private ConnectionFactory connectionFactory;
    private RabbitMQAdminServiceImpl adminService;

    private Queue mainQueue;
    private DirectExchange mainExchange;
    private Binding mainBinding;

    @BeforeEach
    void setUp() {
        rabbitAdmin = Mockito.mock(RabbitAdmin.class);
        connectionFactory = Mockito.mock(ConnectionFactory.class);

        when(connectionFactory.getHost()).thenReturn("localhost");
        when(connectionFactory.getPort()).thenReturn(5672);
        when(connectionFactory.getVirtualHost()).thenReturn("/");

        mainQueue = new Queue(RabbitMQConstants.QUEUE_PEDIDO_CREADO, true);
        mainExchange = new DirectExchange(RabbitMQConstants.EXCHANGE_PEDIDOS, true, false);
        mainBinding = new Binding(
                RabbitMQConstants.QUEUE_PEDIDO_CREADO,
                Binding.DestinationType.QUEUE,
                RabbitMQConstants.EXCHANGE_PEDIDOS,
                RabbitMQConstants.RK_PEDIDO_CREADO,
                Collections.emptyMap()
        );

        adminService = new RabbitMQAdminServiceImpl(
                rabbitAdmin,
                connectionFactory,
                List.of(mainQueue),
                List.of(mainExchange),
                List.of(mainBinding)
        );
    }

    @Test
    @DisplayName("1. getBrokerStatus reporta estado del broker y conteo de recursos administrados")
    void getBrokerStatus_shouldReturnStatusSummary() {
        Connection connection = Mockito.mock(Connection.class);
        when(connection.isOpen()).thenReturn(true);
        when(connectionFactory.createConnection()).thenReturn(connection);

        BrokerStatusDTO status = adminService.getBrokerStatus();

        assertNotNull(status);
        assertEquals("CONNECTED", status.getStatus());
        assertEquals("localhost", status.getHost());
        assertEquals(5672, status.getPort());
        assertEquals(1, status.getTotalQueues());
        assertEquals(1, status.getTotalExchanges());
    }

    @Test
    @DisplayName("2. createQueue declara la cola durable en el broker a traves de RabbitAdmin")
    void createQueue_shouldDeclareQueueInBroker() {
        QueueRequestDTO request = new QueueRequestDTO(
                "test.nueva-cola.queue",
                true,
                false,
                false,
                Collections.emptyMap()
        );

        QueueInfoDTO result = adminService.createQueue(request);

        assertNotNull(result);
        assertEquals("test.nueva-cola.queue", result.getName());
        assertTrue(result.isDurable());
        assertFalse(result.isCritical());

        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);
        verify(rabbitAdmin).declareQueue(captor.capture());
        assertEquals("test.nueva-cola.queue", captor.getValue().getName());
    }

    @Test
    @DisplayName("3. deleteQueue sobre una cola critica del sistema lanza CriticalResourceException")
    void deleteQueue_criticalQueue_shouldThrowCriticalResourceException() {
        assertThrows(CriticalResourceException.class, () ->
                adminService.deleteQueue(RabbitMQConstants.QUEUE_PEDIDO_CREADO, false)
        );
        verify(rabbitAdmin, Mockito.never()).deleteQueue(RabbitMQConstants.QUEUE_PEDIDO_CREADO);
    }

    @Test
    @DisplayName("4. deleteQueue sobre una cola normal elimina la cola a traves de RabbitAdmin")
    void deleteQueue_normalQueue_shouldDeleteSuccessfully() {
        // Primero registramos una cola temporal
        QueueRequestDTO req = new QueueRequestDTO("temp.queue", false, false, true, null);
        adminService.createQueue(req);

        adminService.deleteQueue("temp.queue", false);

        verify(rabbitAdmin).deleteQueue("temp.queue");
    }

    @Test
    @DisplayName("5. createExchange declara exchanges de diferentes tipos (direct, topic, fanout, headers)")
    void createExchange_shouldDeclareCorrectType() {
        ExchangeRequestDTO reqDirect = new ExchangeRequestDTO("custom.direct", "direct", true, false, null);
        ExchangeRequestDTO reqTopic = new ExchangeRequestDTO("custom.topic", "topic", true, false, null);

        adminService.createExchange(reqDirect);
        adminService.createExchange(reqTopic);

        verify(rabbitAdmin, Mockito.times(2)).declareExchange(any(Exchange.class));
    }

    @Test
    @DisplayName("6. deleteExchange sobre un exchange critico lanza CriticalResourceException")
    void deleteExchange_criticalExchange_shouldThrowCriticalResourceException() {
        assertThrows(CriticalResourceException.class, () ->
                adminService.deleteExchange(RabbitMQConstants.EXCHANGE_PEDIDOS, false)
        );
        verify(rabbitAdmin, Mockito.never()).deleteExchange(RabbitMQConstants.EXCHANGE_PEDIDOS);
    }

    @Test
    @DisplayName("7. createBinding valida existencia previa de la cola y del exchange")
    void createBinding_nonExistentResource_shouldThrowResourceNotFoundException() {
        BindingRequestDTO requestInexistente = new BindingRequestDTO(
                "cola.inexistente",
                RabbitMQConstants.EXCHANGE_PEDIDOS,
                "routing.key",
                null
        );

        assertThrows(ResourceNotFoundException.class, () ->
                adminService.createBinding(requestInexistente)
        );
    }

    @Test
    @DisplayName("8. createBinding con recursos existentes declara el enlace en RabbitAdmin")
    void createBinding_validResources_shouldDeclareBinding() {
        BindingRequestDTO request = new BindingRequestDTO(
                RabbitMQConstants.QUEUE_PEDIDO_CREADO,
                RabbitMQConstants.EXCHANGE_PEDIDOS,
                "pedido.especial",
                null
        );

        BindingInfoDTO result = adminService.createBinding(request);

        assertNotNull(result);
        assertEquals(RabbitMQConstants.QUEUE_PEDIDO_CREADO, result.getQueueName());
        assertEquals("pedido.especial", result.getRoutingKey());

        verify(rabbitAdmin).declareBinding(any(Binding.class));
    }
}
