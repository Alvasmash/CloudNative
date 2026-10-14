package cl.duoc.pedidos360.event;

import java.io.Serializable;

/**
 * =========================================================================
 * EVENTO: ItemPedidoEvent (DTO de detalle de item en evento)
 * =========================================================================
 * Representa un item dentro de un evento de negocio desacoplado de la entidad JPA.
 */
public class ItemPedidoEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productoId;
    private String productoNombre;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;

    public ItemPedidoEvent() {
    }

    public ItemPedidoEvent(Long productoId, String productoNombre, Integer cantidad, Double precioUnitario, Double subtotal) {
        this.productoId = productoId;
        this.productoNombre = productoNombre;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public void setProductoNombre(String productoNombre) {
        this.productoNombre = productoNombre;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }

    @Override
    public String toString() {
        return "ItemPedidoEvent{" +
                "productoId=" + productoId +
                ", productoNombre='" + productoNombre + '\'' +
                ", cantidad=" + cantidad +
                ", precioUnitario=" + precioUnitario +
                ", subtotal=" + subtotal +
                '}';
    }
}
