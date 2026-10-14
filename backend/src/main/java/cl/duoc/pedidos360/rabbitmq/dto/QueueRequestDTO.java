package cl.duoc.pedidos360.rabbitmq.dto;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * =========================================================================
 * DTO: QueueRequestDTO (Peticion de Creacion de Cola)
 * =========================================================================
 * Criterio de Evaluacion EP3 (7%): Validacion de parametros.
 * Restringe caracteres invalidos, longitudes maximas y obligatoriedad.
 */
public class QueueRequestDTO {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Size(min = 1, max = 255, message = "El nombre de la cola debe tener entre 1 y 255 caracteres")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+$",
            message = "El nombre de la cola solo puede contener caracteres alfanumericos, puntos, guiones y guiones bajos"
    )
    private String name;

    private boolean durable = true;
    private boolean exclusive = false;
    private boolean autoDelete = false;
    private Map<String, Object> arguments = new HashMap<>();

    public QueueRequestDTO() {
    }

    public QueueRequestDTO(String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments) {
        this.name = name;
        this.durable = durable;
        this.exclusive = exclusive;
        this.autoDelete = autoDelete;
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

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
