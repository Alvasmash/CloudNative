package cl.duoc.pedidos360.kafka;

import java.util.Map;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/kafka")
public class KafkaAdminController {
    private final ObjectProvider<KafkaEventStream> stream;
    private final ObjectProvider<KafkaManagement> management;

    public KafkaAdminController(ObjectProvider<KafkaEventStream> stream, ObjectProvider<KafkaManagement> management) {
        this.stream = stream;
        this.management = management;
    }

    public record TopicRequest(@NotBlank String name, @Positive int partitions, @Positive short replicas,
        @Min(-1) Long retentionMs, @Pattern(regexp = "delete|compact|compact,delete") String cleanupPolicy) {}

    public record TopicSettings(@Min(-1) Long retentionMs,
        @Pattern(regexp = "delete|compact|compact,delete") String cleanupPolicy) {}

    public record Partitions(@Positive int total) {}

    @GetMapping
    public Map<String, Object> status() {
        KafkaEventStream active = stream.getIfAvailable();
        return active == null ? Map.of("status", "DISABLED", "topic", "", "events", java.util.List.of()) : active.status();
    }

    @GetMapping("/topics")
    public List<String> topics() {
        return management().topics();
    }

    @GetMapping("/topics/{name}")
    public Map<String, Object> topic(@PathVariable String name) {
        return management().topic(name);
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody TopicRequest topic) {
        management().create(topic.name(), topic.partitions(), topic.replicas(), topic.retentionMs(), topic.cleanupPolicy());
    }

    @PatchMapping("/topics/{name}/configuration")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void configure(@PathVariable String name, @Valid @RequestBody TopicSettings settings) {
        management().configure(name, settings.retentionMs(), settings.cleanupPolicy());
    }

    @PutMapping("/topics/{name}/partitions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void partitions(@PathVariable String name, @Valid @RequestBody Partitions partitions) {
        management().partitions(name, partitions.total());
    }

    @DeleteMapping("/topics/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String name) {
        management().delete(name);
    }

    @GetMapping("/groups")
    public List<String> groups() {
        return management().groups();
    }

    @GetMapping("/groups/{id}")
    public Map<String, Object> group(@PathVariable String id) {
        return management().group(id);
    }

    private KafkaManagement management() {
        KafkaManagement active = management.getIfAvailable();
        if (active == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kafka desactivado");
        return active;
    }
}
