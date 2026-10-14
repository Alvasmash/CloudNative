package cl.duoc.pedidos360.rabbitmq.dto;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * =========================================================================
 * DTO: ExchangeRequestDTO (Peticion de Creacion de Exchange)
 * =========================================================================
 * Criterio de Evaluacion EP3 (7%): Validacion de parametros.
 * Restringe los tipos a direct, topic, fanout y headers.
 */
public class ExchangeRequestDTO {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Size(min = 1, max = 255, message = "El nombre del exchange debe tener entre 1 y 255 caracteres")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+$",
            message = "El nombre del exchange solo puede contener caracteres alfanumericos, puntos, guiones y guiones bajos"
    )
    private String name;

    @NotBlank(message = "El tipo de exchange es obligatorio")
    @Pattern(
            regexp = "^(?i)(direct|topic|fanout|headers)$",
            message = "Tipo de exchange invalido. Valores soportados: direct, topic, fanout, headers"
    )
    private String type = "direct";

    private boolean durable = true;
    private boolean autoDelete = false;
    private Map<String, Object> arguments = new HashMap<>();

    public ExchangeRequestDTO() {
    }

    public ExchangeRequestDTO(String name, String type, boolean durable, boolean autoDelete, Map<String, Object> arguments) {
        this.name = name;
        this.type = type;
        this.durable = durable;
        this.autoDelete = autoDelete;
        this.arguments = arguments != null ? arguments : new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public boolean isAutoDelete() {
        return autoDelete;
    }

    public void setAutoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
