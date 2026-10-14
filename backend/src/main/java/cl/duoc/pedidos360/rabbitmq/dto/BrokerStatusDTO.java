package cl.duoc.pedidos360.rabbitmq.dto;

/**
 * DTO que describe el estado de conectividad y resumen del broker RabbitMQ
 */
public class BrokerStatusDTO {

    private String status;
    private String host;
    private int port;
    private String virtualHost;
    private int totalQueues;
    private int totalExchanges;
    private int totalBindings;
    private String timestamp;

    public BrokerStatusDTO() {
    }

    public BrokerStatusDTO(String status, String host, int port, String virtualHost,
                           int totalQueues, int totalExchanges, int totalBindings, String timestamp) {
        this.status = status;
        this.host = host;
        this.port = port;
        this.virtualHost = virtualHost;
        this.totalQueues = totalQueues;
        this.totalExchanges = totalExchanges;
        this.totalBindings = totalBindings;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public int getTotalQueues() {
        return totalQueues;
    }

    public void setTotalQueues(int totalQueues) {
        this.totalQueues = totalQueues;
    }

    public int getTotalExchanges() {
        return totalExchanges;
    }

    public void setTotalExchanges(int totalExchanges) {
        this.totalExchanges = totalExchanges;
    }

    public int getTotalBindings() {
        return totalBindings;
    }

    public void setTotalBindings(int totalBindings) {
        this.totalBindings = totalBindings;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
