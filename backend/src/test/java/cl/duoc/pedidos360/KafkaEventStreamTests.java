package cl.duoc.pedidos360;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cl.duoc.pedidos360.event.PedidoLifecycleEvent;
import cl.duoc.pedidos360.kafka.KafkaEventStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaEventStreamTests {
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    private final KafkaEventStream stream = new KafkaEventStream(kafka, json, "pedidos.eventos", "localhost:9092");
    private PedidoLifecycleEvent event(int id) {
        return new PedidoLifecycleEvent("event-" + id, "PEDIDO_CREADO", (long) id, "PED-" + id, "PENDIENTE", 8990.0, Instant.now());
    }

    @Test void brokerFailureDoesNotFailCommittedOrder() {
        when(kafka.send(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.<SendResult<String, String>>failedFuture(new RuntimeException("broker offline")));
        assertDoesNotThrow(() -> stream.publish(event(42)));
        verify(kafka).send(eq("pedidos.eventos"), eq("42"), contains("PEDIDO_CREADO"));
    }

    @Test void immediateFailureDoesNotEscapePublisher() {
        when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("metadata unavailable"));
        assertDoesNotThrow(() -> stream.publish(event(42)));
    }

    @Test void auditConsumerRetainsLatestFiftyWithKafkaCoordinates() throws Exception {
        for (int i = 0; i < 55; i++) {
            stream.consume(new ConsumerRecord<>("pedidos.eventos", 2, i, String.valueOf(i), json.writeValueAsString(event(i))));
        }
        assertEquals(50, stream.recentEvents().size());
        assertEquals(54, stream.recentEvents().getFirst().offset());
        assertEquals(2, stream.recentEvents().getFirst().partition());
        assertEquals(54L, stream.recentEvents().getFirst().event().pedidoId());
    }

    @Test void malformedAuditRecordDoesNotBlockFollowingValidRecord() throws Exception {
        stream.consume(new ConsumerRecord<>("pedidos.eventos", 0, 0, "bad", "invalid json"));
        stream.consume(new ConsumerRecord<>("pedidos.eventos", 0, 1, "bad", "{}"));
        stream.consume(new ConsumerRecord<>("pedidos.eventos", 0, 2, "1", json.writeValueAsString(event(1))));
        assertEquals(1, stream.recentEvents().size());
        assertEquals(2, stream.recentEvents().getFirst().offset());
    }
}
