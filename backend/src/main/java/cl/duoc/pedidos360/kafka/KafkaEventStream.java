package cl.duoc.pedidos360.kafka;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import cl.duoc.pedidos360.event.PedidoLifecycleEvent;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaEventStream {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventStream.class);
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final String topic;
    private final String brokers;
    private final ArrayDeque<ReceivedEvent> recent = new ArrayDeque<>();

    public record ReceivedEvent(PedidoLifecycleEvent event, int partition, long offset) {}

    public KafkaEventStream(KafkaTemplate<String, String> kafka, ObjectMapper json,
            @Value("${app.kafka.topic}") String topic,
            @Value("${spring.kafka.bootstrap-servers}") String brokers) {
        this.kafka = kafka;
        this.json = json;
        this.topic = topic;
        this.brokers = brokers;
    }

    @Bean
    NewTopic pedidosEventTopic() {
        return TopicBuilder.name(topic).partitions(3).replicas(1)
            .config("retention.ms", "604800000").build();
    }

    @EventListener
    public void publish(PedidoLifecycleEvent event) {
        try {
            kafka.send(topic, String.valueOf(event.pedidoId()), json.writeValueAsString(event))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Kafka: no se pudo publicar evento {} para pedido {}. El pedido ya fue guardado: {}",
                            event.eventId(), event.pedidoId(), error.getMessage());
                    } else {
                        log.info("Kafka publicado: {} pedido={} partition={} offset={}", event.type(), event.pedidoId(),
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
        } catch (Exception error) {
            // Audit transport failure must not turn a committed order into an HTTP error.
            log.error("Kafka no disponible para evento {}: {}", event.eventId(), error.getMessage());
        }
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "pedidos360-auditoria")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            PedidoLifecycleEvent event = json.readValue(record.value(), PedidoLifecycleEvent.class);
            if (event == null || event.eventId() == null || event.pedidoId() == null || event.type() == null) {
                throw new IllegalArgumentException("Evento incompleto");
            }
            synchronized (recent) {
                recent.addFirst(new ReceivedEvent(event, record.partition(), record.offset()));
                while (recent.size() > 50) recent.removeLast();
            }
            log.info("Kafka consumido: {} pedido={} partition={} offset={}", event.type(), event.pedidoId(), record.partition(), record.offset());
        } catch (Exception error) {
            // Keep malformed audit records from blocking later valid records.
            log.warn("Kafka: evento invalido descartado partition={} offset={}: {}", record.partition(), record.offset(), error.getMessage());
        }
    }

    public Map<String, Object> status() {
        String status = "DISCONNECTED";
        try (AdminClient admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 1500,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 2000))) {
            if (!admin.describeCluster().nodes().get(2, TimeUnit.SECONDS).isEmpty()) status = "CONNECTED";
        } catch (Exception error) {
            log.debug("Kafka health check: {}", error.getMessage());
        }
        return Map.of("status", status, "topic", topic, "events", recentEvents(), "retentionDays", 7);
    }

    public List<ReceivedEvent> recentEvents() {
        synchronized (recent) { return new ArrayList<>(recent); }
    }
}
