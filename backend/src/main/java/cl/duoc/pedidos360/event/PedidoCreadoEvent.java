package cl.duoc.pedidos360.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================
 * EVENTO: PedidoCreadoEvent (DTO de Evento de Dominio)
 * =========================================================================
 * Representa el evento de negocio emitido tras la confirmacion de un pedido.
 * Desacoplado de la entidad JPA Pedido para evitar exponer relaciones de BD
 * o problemas de carga perezosa (lazy loading) durante la serializacion.
 */
public class PedidoCreadoEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String numeroPedido;
    private String clienteNombre;
    private String clienteEmail;
    private Double total;
    private String estado;
    private LocalDateTime fechaCreacion;
    private List<ItemPedidoEvent> items = new ArrayList<>();

    public PedidoCreadoEvent() {
    }

    public PedidoCreadoEvent(Long id, String numeroPedido, String clienteNombre, String clienteEmail,
                             Double total, String estado, LocalDateTime fechaCreacion, List<ItemPedidoEvent> items) {
        this.id = id;
        this.numeroPedido = numeroPedido;
        this.clienteNombre = clienteNombre;
        this.clienteEmail = clienteEmail;
        this.total = total;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.items = items != null ? items : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public void setNumeroPedido(String numeroPedido) {
        this.numeroPedido = numeroPedido;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public List<ItemPedidoEvent> getItems() {
        return items;
    }

    public void setItems(List<ItemPedidoEvent> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        return "PedidoCreadoEvent{" +
                "id=" + id +
                ", numeroPedido='" + numeroPedido + '\'' +
                ", clienteNombre='" + clienteNombre + '\'' +
                ", clienteEmail='" + clienteEmail + '\'' +
                ", total=" + total +
                ", estado='" + estado + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                ", itemsCount=" + (items != null ? items.size() : 0) +
                '}';
    }
}
