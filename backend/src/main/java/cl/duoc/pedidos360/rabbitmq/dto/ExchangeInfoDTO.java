package cl.duoc.pedidos360.rabbitmq.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO que describe el estado y configuracion de un exchange.
 */
public class ExchangeInfoDTO {

    private String name;
    private String type;
    private boolean durable;
    private boolean autoDelete;
    private boolean isCritical;
    private boolean verifiedInBroker;
    private Map<String, Object> arguments = new HashMap<>();

    public ExchangeInfoDTO() {
    }

    public ExchangeInfoDTO(String name, String type, boolean durable, boolean autoDelete,
                           boolean isCritical, boolean verifiedInBroker, Map<String, Object> arguments) {
        this.name = name;
        this.type = type;
        this.durable = durable;
        this.autoDelete = autoDelete;
        this.isCritical = isCritical;
        this.verifiedInBroker = verifiedInBroker;
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

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
