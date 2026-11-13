package cl.duoc.pedidos360.kafka;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Service;
import org.springframework.util.backoff.FixedBackOff;
import com.fasterxml.jackson.databind.ObjectMapper;
import cl.duoc.pedidos360.event.PedidoLifecycleEvent;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaEventStream {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventStream.class);
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final String topic;
    private final String salesTopic;
    private final int replicas;
    private final Admin admin;
    private final ArrayDeque<ReceivedEvent> recent = new ArrayDeque<>();

    public record ReceivedEvent(PedidoLifecycleEvent event, int partition, long offset) {}

    public KafkaEventStream(KafkaTemplate<String, String> kafka, ObjectMapper json,
            @Value("${app.kafka.topic}") String topic,
            @Value("${app.kafka.sales-topic}") String salesTopic,
            @Value("${app.kafka.replicas}") int replicas, Admin admin) {
        this.kafka = kafka;
        this.json = json;
        this.topic = topic;
        this.salesTopic = salesTopic;
        this.replicas = replicas;
        this.admin = admin;
    }

    @Bean
    NewTopic pedidosEventTopic() {
        return topic(topic, "604800000");
    }

    @Bean
    NewTopic salesTopic() {
        return topic(salesTopic, "2592000000");
    }

    @Bean
    DefaultErrorHandler auditErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(1000, 2));
    }

    private NewTopic topic(String name, String retentionMs) {
        return TopicBuilder.name(name).partitions(3).replicas(replicas)
            .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
            .config(TopicConfig.RETENTION_MS_CONFIG, retentionMs)
            .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1").build();
    }

    @EventListener
    public void publish(PedidoLifecycleEvent event) {
        send(topic, event.pedidoId(), event.eventId(), event);
        if ("PEDIDO_CREADO".equals(event.type())) {
            if (event.total() == null || !Double.isFinite(event.total()) || event.total() < 0) {
                log.warn("Kafka: importe inválido topic={} eventId={} pedido={}", salesTopic, event.eventId(), event.pedidoId());
                return;
            }
            send(salesTopic, event.pedidoId(), event.eventId(), PedidoSaleEvent.from(event));
        }
    }

    private void send(String destination, Long pedidoId, String eventId, Object event) {
        try {
            kafka.send(destination, pedidoId.toString(), json.writeValueAsString(event))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Kafka: fallo de publicación topic={} eventId={} pedido={}", destination, eventId, pedidoId, error);
                    } else {
                        log.info("Kafka publicado: topic={} pedido={} partition={} offset={}", destination, pedidoId,
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
        } catch (Exception error) {
            // Audit transport failure must not turn a committed order into an HTTP error.
            log.error("Kafka: fallo de publicación topic={} eventId={} pedido={}", destination, eventId, pedidoId, error);
        }
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${app.kafka.audit-group}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            PedidoLifecycleEvent event = json.readValue(record.value(), PedidoLifecycleEvent.class);
            if (event == null || event.eventId() == null || event.eventId().isBlank() || event.pedidoId() == null
                    || !event.pedidoId().toString().equals(record.key()) || event.occurredAt() == null
                    || event.type() == null || !List.of("PEDIDO_CREADO", "ESTADO_ACTUALIZADO", "PEDIDO_ELIMINADO").contains(event.type())) {
                throw new IllegalArgumentException("Evento incompleto");
            }
            synchronized (recent) {
                recent.addFirst(new ReceivedEvent(event, record.partition(), record.offset()));
                while (recent.size() > 50) recent.removeLast();
            }
            log.info("Kafka consumido: {} pedido={} partition={} offset={}", event.type(), event.pedidoId(), record.partition(), record.offset());
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException error) {
            // Keep malformed audit records from blocking later valid records.
            log.warn("Kafka: evento inválido descartado topic={} partition={} offset={}", record.topic(), record.partition(), record.offset());
        }
    }

    public Map<String, Object> status() {
        String status = "DISCONNECTED";
        try {
            if (!admin.describeCluster().nodes().get(2, TimeUnit.SECONDS).isEmpty()) status = "CONNECTED";
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception error) {
            log.debug("Kafka health check: {}", error.getMessage());
        }
        return Map.of("status", status, "topic", topic, "salesTopic", salesTopic, "events", recentEvents());
    }

    public List<ReceivedEvent> recentEvents() {
        synchronized (recent) { return new ArrayList<>(recent); }
    }
}
