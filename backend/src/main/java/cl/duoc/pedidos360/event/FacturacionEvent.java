package cl.duoc.pedidos360.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * =========================================================================
 * EVENTO: FacturacionEvent (Dominio de Facturacion y Tributario)
 * =========================================================================
 * Representa la emision y calculo de Documentos Tributarios Electronicos (DTE / Boleta)
 * de forma asincrona para cumplir con los estandares fiscales.
 */
public class FacturacionEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long pedidoId;
    private String numeroPedido;
    private String folioFiscal;
    private String clienteNombre;
    private String clienteEmail;
    private Double montoNeto;
    private Double montoIva;
    private Double montoTotal;
    private LocalDateTime fechaEmision;

    public FacturacionEvent() {
    }

    public FacturacionEvent(Long pedidoId, String numeroPedido, String folioFiscal,
                            String clienteNombre, String clienteEmail,
                            Double montoNeto, Double montoIva, Double montoTotal,
                            LocalDateTime fechaEmision) {
        this.pedidoId = pedidoId;
        this.numeroPedido = numeroPedido;
        this.folioFiscal = folioFiscal;
        this.clienteNombre = clienteNombre;
        this.clienteEmail = clienteEmail;
        this.montoNeto = montoNeto;
        this.montoIva = montoIva;
        this.montoTotal = montoTotal;
        this.fechaEmision = fechaEmision != null ? fechaEmision : LocalDateTime.now();
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

    public String getFolioFiscal() {
        return folioFiscal;
    }

    public void setFolioFiscal(String folioFiscal) {
        this.folioFiscal = folioFiscal;
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

    public Double getMontoNeto() {
        return montoNeto;
    }

    public void setMontoNeto(Double montoNeto) {
        this.montoNeto = montoNeto;
    }

    public Double getMontoIva() {
        return montoIva;
    }

    public void setMontoIva(Double montoIva) {
        this.montoIva = montoIva;
    }

    public Double getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(Double montoTotal) {
        this.montoTotal = montoTotal;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    @Override
    public String toString() {
        return "FacturacionEvent{" +
                "pedidoId=" + pedidoId +
                ", numeroPedido='" + numeroPedido + '\'' +
                ", folioFiscal='" + folioFiscal + '\'' +
                ", montoTotal=" + montoTotal +
                ", fechaEmision=" + fechaEmision +
                '}';
    }
}
