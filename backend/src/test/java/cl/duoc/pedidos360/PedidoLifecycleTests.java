package cl.duoc.pedidos360;

import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import cl.duoc.pedidos360.event.PedidoLifecycleEvent;
import cl.duoc.pedidos360.model.*;
import cl.duoc.pedidos360.repository.*;
import cl.duoc.pedidos360.producer.PedidoEventProducer;
import cl.duoc.pedidos360.service.PedidoService;
import static org.mockito.Mockito.*;

class PedidoLifecycleTests {
    private final PedidoRepository orders = mock(PedidoRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final PedidoService service = new PedidoService(orders, mock(ProductoRepository.class), mock(PedidoEventProducer.class), events);
    private final Pedido order = new Pedido();

    @BeforeEach void setUp() {
        order.setId(1L); order.setNumeroPedido("PED-LOCAL"); order.setEstado(EstadoPedido.PENDIENTE); order.setTotal(8990.0);
        when(orders.findById(1L)).thenReturn(Optional.of(order));
        when(orders.save(order)).thenReturn(order);
        TransactionSynchronizationManager.initSynchronization();
    }
    @AfterEach void tearDown() { TransactionSynchronizationManager.clearSynchronization(); }

    @Test void stateChangePublishesOnlyAfterCommit() {
        service.actualizarEstado(1L, EstadoPedido.EN_PREPARACION);
        verifyNoInteractions(events);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        verify(events).publishEvent(argThat((PedidoLifecycleEvent event) ->
            event.type().equals("ESTADO_ACTUALIZADO") && event.estado().equals("EN_PREPARACION") && event.pedidoId() == 1L));
    }

    @Test void rollbackDoesNotPublishKafkaAudit() {
        service.actualizarEstado(1L, EstadoPedido.CANCELADO);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(1));
        verifyNoInteractions(events);
    }

    @Test void deletionPublishesImmutableOrderSnapshotAfterCommit() {
        service.eliminarPedido(1L);
        verify(orders).deleteById(1L);
        verifyNoInteractions(events);
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
        verify(events).publishEvent(argThat((PedidoLifecycleEvent event) ->
            event.type().equals("PEDIDO_ELIMINADO") && event.numeroPedido().equals("PED-LOCAL")));
    }
}
