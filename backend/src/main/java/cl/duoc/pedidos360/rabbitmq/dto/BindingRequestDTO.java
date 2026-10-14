package cl.duoc.pedidos360.rabbitmq.dto;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * =========================================================================
 * DTO: BindingRequestDTO (Peticion de Enlace entre Cola y Exchange)
 * =========================================================================
 * Criterio de Evaluacion EP3 (7%): Validacion de parametros.
 */
public class BindingRequestDTO {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+$",
            message = "El nombre de la cola contiene caracteres invalidos"
    )
    private String queueName;

    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+$",
            message = "El nombre del exchange contiene caracteres invalidos"
    )
    private String exchangeName;

    @NotNull(message = "La routing key es obligatoria (puede ser vacia para fanout pero no nula)")
    private String routingKey = "";

    private Map<String, Object> arguments = new HashMap<>();

    public BindingRequestDTO() {
    }

    public BindingRequestDTO(String queueName, String exchangeName, String routingKey, Map<String, Object> arguments) {
        this.queueName = queueName;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey != null ? routingKey : "";
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
