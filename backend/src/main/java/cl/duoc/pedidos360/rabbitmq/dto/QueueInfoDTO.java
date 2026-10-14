package cl.duoc.pedidos360.rabbitmq.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO que describe el estado y configuracion de una cola.
 * Distingue si la existencia y metricas fueron verificadas en vivo en el broker.
 */
public class QueueInfoDTO {

    private String name;
    private boolean durable;
    private boolean exclusive;
    private boolean autoDelete;
    private boolean isCritical;
    private boolean verifiedInBroker;
    private Integer messageCount;
    private Integer consumerCount;
    private Map<String, Object> arguments = new HashMap<>();

    public QueueInfoDTO() {
    }

    public QueueInfoDTO(String name, boolean durable, boolean exclusive, boolean autoDelete,
                        boolean isCritical, boolean verifiedInBroker, Integer messageCount,
                        Integer consumerCount, Map<String, Object> arguments) {
        this.name = name;
        this.durable = durable;
        this.exclusive = exclusive;
        this.autoDelete = autoDelete;
        this.isCritical = isCritical;
        this.verifiedInBroker = verifiedInBroker;
        this.messageCount = messageCount;
        this.consumerCount = consumerCount;
        this.arguments = arguments != null ? arguments : new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public boolean isAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    public boolean isCritical() {
        return isCritical;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }

    public boolean isVerifiedInBroker() {
        return verifiedInBroker;
    }

    public void setVerifiedInBroker(boolean verifiedInBroker) {
        this.verifiedInBroker = verifiedInBroker;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }

    public Integer getConsumerCount() {
        return consumerCount;
    }

    public void setConsumerCount(Integer consumerCount) {
        this.consumerCount = consumerCount;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
