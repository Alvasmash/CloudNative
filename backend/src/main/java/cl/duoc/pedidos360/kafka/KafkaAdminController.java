package cl.duoc.pedidos360.kafka;

import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/kafka")
public class KafkaAdminController {
    private final ObjectProvider<KafkaEventStream> stream;
    public KafkaAdminController(ObjectProvider<KafkaEventStream> stream) { this.stream = stream; }
    @GetMapping
    public Map<String, Object> status() {
        KafkaEventStream active = stream.getIfAvailable();
        return active == null ? Map.of("status", "DISABLED", "topic", "", "events", java.util.List.of()) : active.status();
    }
}
