package cl.duoc.pedidos360.rabbitmq.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO que describe un enlace (binding) entre una cola y un exchange
 */
public class BindingInfoDTO {

    private String queueName;
    private String exchangeName;
    private String routingKey;
    private Map<String, Object> arguments = new HashMap<>();

    public BindingInfoDTO() {
    }

    public BindingInfoDTO(String queueName, String exchangeName, String routingKey, Map<String, Object> arguments) {
        this.queueName = queueName;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
        this.arguments = arguments != null ? arguments : new HashMap<>();
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
