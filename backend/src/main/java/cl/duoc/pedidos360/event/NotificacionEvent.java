package cl.duoc.pedidos360.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * =========================================================================
 * EVENTO: NotificacionEvent (Dominio de Notificaciones)
 * =========================================================================
 * Representa la solicitud asincrona de envio de notificaciones (correo / SMS / push)
 * al cliente sobre el estado y avance de su pedido.
 */
public class NotificacionEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long pedidoId;
    private String numeroPedido;
    private String clienteNombre;
    private String clienteEmail;
    private String tipoNotificacion;
    private String mensaje;
    private LocalDateTime timestamp;

    public NotificacionEvent() {
    }

    public NotificacionEvent(Long pedidoId, String numeroPedido, String clienteNombre,
                             String clienteEmail, String tipoNotificacion, String mensaje,
                             LocalDateTime timestamp) {
        this.pedidoId = pedidoId;
        this.numeroPedido = numeroPedido;
        this.clienteNombre = clienteNombre;
        this.clienteEmail = clienteEmail;
        this.tipoNotificacion = tipoNotificacion;
        this.mensaje = mensaje;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
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

    public String getTipoNotificacion() {
        return tipoNotificacion;
    }

    public void setTipoNotificacion(String tipoNotificacion) {
        this.tipoNotificacion = tipoNotificacion;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "NotificacionEvent{" +
                "pedidoId=" + pedidoId +
                ", numeroPedido='" + numeroPedido + '\'' +
                ", clienteEmail='" + clienteEmail + '\'' +
                ", tipoNotificacion='" + tipoNotificacion + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
