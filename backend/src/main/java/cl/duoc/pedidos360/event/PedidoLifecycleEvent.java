package cl.duoc.pedidos360.event;

import java.time.Instant;
import java.util.UUID;
import cl.duoc.pedidos360.model.Pedido;

/** Audit envelope: deliberately excludes customer personal information. */
public record PedidoLifecycleEvent(String eventId, String type, Long pedidoId,
        String numeroPedido, String estado, Double total, Instant occurredAt) {
    public static PedidoLifecycleEvent from(String type, Pedido pedido) {
        return new PedidoLifecycleEvent(UUID.randomUUID().toString(), type, pedido.getId(),
            pedido.getNumeroPedido(), pedido.getEstado().name(), pedido.getTotal(), Instant.now());
    }
}
