package cl.duoc.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cl.duoc.pedidos360.kafka.PedidoSaleEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

class SalesConsumer {
    private static final Logger log = LoggerFactory.getLogger(SalesConsumer.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Santiago");
    private static final BigDecimal TAX_FACTOR = new BigDecimal("1.19");
    private final ObjectMapper json;

    SalesConsumer(ObjectMapper json) {
        this.json = json;
    }

    @KafkaListener(topics = "${app.kafka.sales-topic}", groupId = "${app.kafka.sales-group}")
    void consume(ConsumerRecord<String, String> record) {
        PedidoSaleEvent sale;
        try {
            sale = json.readValue(record.value(), PedidoSaleEvent.class);
            if (sale == null || sale.eventId() == null || sale.eventId().isBlank()
                    || sale.pedidoId() == null || !sale.pedidoId().toString().equals(record.key())
                    || sale.occurredAt() == null || sale.totalClp() == null || sale.totalClp().signum() < 0) {
                throw new IllegalArgumentException("Venta incompleta");
            }
        } catch (JsonProcessingException | IllegalArgumentException error) {
            log.warn("Kafka: venta inválida descartada topic={} partition={} offset={}",
                record.topic(), record.partition(), record.offset());
            return;
        }

        BigDecimal net = sale.totalClp().divide(TAX_FACTOR, 2, RoundingMode.HALF_UP);
        log.info("Venta recibida fecha={} eventId={} pedido={} brutoClp={} netoClp={} ivaClp={} partition={} offset={}",
            sale.occurredAt().atZone(BUSINESS_ZONE).toLocalDate(), sale.eventId(), sale.pedidoId(),
            sale.totalClp(), net, sale.totalClp().subtract(net), record.partition(), record.offset());
    }
}
