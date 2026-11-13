package cl.duoc.pedidos360.kafka;

import java.math.BigDecimal;
import java.time.Instant;

public record PedidoSaleEvent(String eventId, Long pedidoId, BigDecimal totalClp, Instant occurredAt) {
    static PedidoSaleEvent from(cl.duoc.pedidos360.event.PedidoLifecycleEvent event) {
        return new PedidoSaleEvent(event.eventId(), event.pedidoId(),
            BigDecimal.valueOf(event.total()), event.occurredAt());
    }
}
