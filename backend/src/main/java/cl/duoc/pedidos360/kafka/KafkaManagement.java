package cl.duoc.pedidos360.kafka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
class KafkaManagement {
    private final Admin admin;
    private final String auditTopic;
    private final String salesTopic;

    KafkaManagement(Admin admin, @Value("${app.kafka.topic}") String auditTopic,
            @Value("${app.kafka.sales-topic}") String salesTopic) {
        this.admin = admin;
        this.auditTopic = auditTopic;
        this.salesTopic = salesTopic;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
    static class ClientConfiguration {
        @Bean
        Admin managementClient(KafkaAdmin kafkaAdmin) {
            var properties = new java.util.HashMap<>(kafkaAdmin.getConfigurationProperties());
            properties.put("default.api.timeout.ms", 5000);
            properties.put("request.timeout.ms", 4000);
            return Admin.create(properties);
        }
    }

    List<String> topics() {
        return call(admin.listTopics().names()).stream().sorted().toList();
    }

    Map<String, Object> topic(String name) {
        ConfigResource resource = resource(name);
        var topic = call(admin.describeTopics(List.of(name)).topicNameValues().get(name));
        Map<String, String> config = new TreeMap<>();
        call(admin.describeConfigs(List.of(resource)).values().get(resource)).entries().stream()
            .filter(entry -> entry.value() != null && !entry.isSensitive())
            .forEach(entry -> config.put(entry.name(), entry.value()));
        return Map.of("name", name, "partitions", topic.partitions().stream().map(partition -> Map.of(
            "id", partition.partition(),
            "leader", partition.leader() == null ? -1 : partition.leader().id(),
            "replicas", partition.replicas().stream().map(node -> node.id()).toList(),
            "inSyncReplicas", partition.isr().stream().map(node -> node.id()).toList())).toList(),
            "configuration", config);
    }

    void create(String name, int partitions, short replicas, Long retentionMs, String cleanupPolicy) {
        resource(name);
        Map<String, String> configs = new TreeMap<>();
        if (retentionMs != null) configs.put(TopicConfig.RETENTION_MS_CONFIG, retentionMs.toString());
        if (cleanupPolicy != null) configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy);
        call(admin.createTopics(List.of(new NewTopic(name, partitions, replicas).configs(configs))).all());
    }

    void partitions(String name, int total) {
        resource(name);
        call(admin.createPartitions(Map.of(name, NewPartitions.increaseTo(total))).all());
    }

    void configure(String name, Long retentionMs, String cleanupPolicy) {
        ConfigResource resource = resource(name);
        Collection<AlterConfigOp> changes = new ArrayList<>();
        if (retentionMs != null) {
            changes.add(new AlterConfigOp(new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, retentionMs.toString()),
                AlterConfigOp.OpType.SET));
        }
        if (cleanupPolicy != null) {
            changes.add(new AlterConfigOp(new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy),
                AlterConfigOp.OpType.SET));
        }
        if (changes.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica una configuración");
        call(admin.incrementalAlterConfigs(Map.of(resource, changes)).all());
    }

    void delete(String name) {
        resource(name);
        if (name.equals(auditTopic) || name.equals(salesTopic)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tópico requerido por Pedidos360");
        }
        call(admin.deleteTopics(List.of(name)).all());
    }

    List<String> groups() {
        return call(admin.listConsumerGroups().all()).stream().map(group -> group.groupId()).sorted().toList();
    }

    Map<String, Object> group(String id) {
        var group = call(admin.describeConsumerGroups(List.of(id)).describedGroups().get(id));
        var offsets = call(admin.listConsumerGroupOffsets(id).partitionsToOffsetAndMetadata());
        return Map.of("id", id, "state", group.state().toString(), "members", group.members().stream()
            .map(member -> Map.of("id", member.consumerId(), "clientId", member.clientId(),
                "partitions", member.assignment().topicPartitions().stream().map(Object::toString).sorted().toList()))
            .toList(), "offsets", offsets.entrySet().stream().map(entry -> Map.of(
                "topic", entry.getKey().topic(), "partition", entry.getKey().partition(),
                "offset", entry.getValue().offset())).toList());
    }

    private ConfigResource resource(String name) {
        if (!name.matches("[a-zA-Z0-9._-]{1,249}") || name.equals(".") || name.equals("..") || name.startsWith("__")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de tópico inválido o reservado");
        }
        return new ConfigResource(ConfigResource.Type.TOPIC, name);
    }

    private <T> T call(KafkaFuture<T> result) {
        try {
            return result.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Consulta a Kafka interrumpida", interrupted);
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof UnknownTopicOrPartitionException || cause instanceof GroupIdNotFoundException) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso Kafka inexistente", cause);
            }
            if (cause instanceof TopicExistsException) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El tópico ya existe", cause);
            }
            if (cause instanceof InvalidConfigurationException || cause instanceof InvalidPartitionsException
                    || cause instanceof InvalidReplicationFactorException) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, cause.getMessage(), cause);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kafka no completó la operación", cause);
        }
    }
}
