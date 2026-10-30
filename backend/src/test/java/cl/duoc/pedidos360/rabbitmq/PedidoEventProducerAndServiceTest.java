package cl.duoc.pedidos360.rabbitmq;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import cl.duoc.pedidos360.config.RabbitMQConstants;
import cl.duoc.pedidos360.dto.ItemPedidoDTO;
import cl.duoc.pedidos360.dto.PedidoCreateDTO;
import cl.duoc.pedidos360.event.PedidoCreadoEvent;
import cl.duoc.pedidos360.model.EstadoPedido;
import cl.duoc.pedidos360.model.Pedido;
import cl.duoc.pedidos360.model.Producto;
import cl.duoc.pedidos360.producer.PedidoEventProducer;
import cl.duoc.pedidos360.repository.PedidoRepository;
import cl.duoc.pedidos360.repository.ProductoRepository;
import cl.duoc.pedidos360.service.PedidoService;

/**
 * =========================================================================
 * PRUEBAS UNITARIAS: PedidoEventProducer y PedidoService
 * =========================================================================
 * Criterio de Evaluacion EP3 (5.3):
 * 1. Creacion de un pedido y publicacion del evento correspondiente.
 * 2. Uso de DTO especifico de evento desacoplado del modelo JPA.
 * 3. Publicacion al exchange correcto (pedidos.exchange) con routing key (pedido.creado).
 */
class PedidoEventProducerAndServiceTest {

    private RabbitTemplate rabbitTemplate;
    private PedidoEventProducer producer;
    private PedidoRepository pedidoRepository;
    private ProductoRepository productoRepository;
    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        rabbitTemplate = Mockito.mock(RabbitTemplate.class);
        producer = new PedidoEventProducer(rabbitTemplate);

        pedidoRepository = Mockito.mock(PedidoRepository.class);
        productoRepository = Mockito.mock(ProductoRepository.class);
        pedidoService = new PedidoService(pedidoRepository, productoRepository, producer,
            Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
    }

    @Test
    @DisplayName("1. PedidoEventProducer publica evento al exchange correcto con routing key correcta")
    void producer_shouldPublishToCorrectExchangeAndRoutingKey() {
        PedidoCreadoEvent event = new PedidoCreadoEvent();
        event.setId(10L);
        event.setNumeroPedido("PED-12345678");
        event.setClienteNombre("Ana Morales");
        event.setClienteEmail("ana@duocuc.cl");
        event.setTotal(8990.0);
        event.setEstado(EstadoPedido.PENDIENTE.name());

        producer.publicarPedidoCreado(event);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE_PEDIDOS),
                eq(RabbitMQConstants.RK_PEDIDO_CREADO),
                eq(event),
                any(MessagePostProcessor.class)
        );
    }

    @Test
    @DisplayName("2. PedidoService.crearPedido persiste la orden y dispara publicacion del evento de dominio")
    void crearPedido_shouldPersistAndPublishEvent() {
        Producto pizza = new Producto(1L, "Pizza Margherita", "Salsa y queso", 8990.0, "Pizzas", 20);
        when(productoRepository.findActiveForUpdate(1L)).thenReturn(Optional.of(pizza));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(99L);
            return p;
        });

        PedidoCreateDTO dto = new PedidoCreateDTO(
                "Estudiante Duoc",
                "estudiante@duocuc.cl",
                List.of(new ItemPedidoDTO(1L, 2))
        );

        Pedido resultado = pedidoService.crearPedido(dto, "estudiante@duocuc.cl");

        assertNotNull(resultado);
        assertEquals(99L, resultado.getId());
        assertEquals(18, pizza.getStock()); // Se descontaron 2 unidades (20 - 2 = 18)

        // Verificar que se publico el evento
        ArgumentCaptor<PedidoCreadoEvent> eventCaptor = ArgumentCaptor.forClass(PedidoCreadoEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE_PEDIDOS),
                eq(RabbitMQConstants.RK_PEDIDO_CREADO),
                eventCaptor.capture(),
                any(MessagePostProcessor.class)
        );

        PedidoCreadoEvent published = eventCaptor.getValue();
        assertEquals(99L, published.getId());
        assertEquals("Estudiante Duoc", published.getClienteNombre());
        assertEquals(17980.0, published.getTotal());
        assertEquals(1, published.getItems().size());
        assertEquals("Pizza Margherita", published.getItems().get(0).getProductoNombre());

        // Verificar que tambien se emitieron eventos para Cola 2 (Notificacion) y Cola 3 (Facturacion)
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE_PEDIDOS),
                eq(RabbitMQConstants.RK_NOTIFICACION),
                any(cl.duoc.pedidos360.event.NotificacionEvent.class),
                any(MessagePostProcessor.class)
        );

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE_PEDIDOS),
                eq(RabbitMQConstants.RK_FACTURACION),
                any(cl.duoc.pedidos360.event.FacturacionEvent.class),
                any(MessagePostProcessor.class)
        );
    }
}
