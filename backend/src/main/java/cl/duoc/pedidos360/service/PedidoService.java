package cl.duoc.pedidos360.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cl.duoc.pedidos360.dto.ItemPedidoDTO;
import cl.duoc.pedidos360.dto.PedidoCreateDTO;
import cl.duoc.pedidos360.event.ItemPedidoEvent;
import cl.duoc.pedidos360.event.PedidoCreadoEvent;
import cl.duoc.pedidos360.model.EstadoPedido;
import cl.duoc.pedidos360.model.ItemPedido;
import cl.duoc.pedidos360.model.Pedido;
import cl.duoc.pedidos360.model.Producto;
import cl.duoc.pedidos360.producer.PedidoEventProducer;
import cl.duoc.pedidos360.repository.PedidoRepository;
import cl.duoc.pedidos360.repository.ProductoRepository;

/**
 * =========================================================================
 * SERVICIO: PedidoService
 * =========================================================================
 * FUNCION: Controla el flujo transaccional de ordenes de compra en Pedidos360:
 *          - Validacion de existencia de productos en el catalogo.
 *          - Verificacion y descuento automatico de stock en cocina.
 *          - Calculo matematico de subtotales por item y total del pedido.
 *          - Asignacion de codigo unico y estado inicial PENDIENTE.
 *          - Emision transaccional de eventos de negocio hacia RabbitMQ tras el commit.
 * CONECTA CON: PedidoRepository, ProductoRepository, PedidoEventProducer, PedidoController y AdminController.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final PedidoEventProducer pedidoEventProducer;
    private final org.springframework.context.ApplicationEventPublisher domainEvents;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository,
            PedidoEventProducer pedidoEventProducer,
            org.springframework.context.ApplicationEventPublisher domainEvents) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.pedidoEventProducer = pedidoEventProducer;
        this.domainEvents = domainEvents;
    }

    // Listar todos los pedidos (usado por administracion)
    @Transactional(readOnly = true)
    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPorUsuario(String subject) {
        return pedidoRepository.findByOwnerSubjectOrderByFechaCreacionDesc(subject);
    }

    @Transactional(readOnly = true)
    public Optional<Pedido> buscarPorUsuario(Long id, String subject) {
        return pedidoRepository.findByIdAndOwnerSubject(id, subject);
    }

    // Buscar una orden por su ID
    @Transactional(readOnly = true)
    public Optional<Pedido> buscarPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    // Historial de compras filtrado por correo del cliente
    @Transactional(readOnly = true)
    public List<Pedido> listarPorCliente(String clienteEmail) {
        return pedidoRepository.findByClienteEmailOrderByFechaCreacionDesc(clienteEmail);
    }

    /**
     * Crea un pedido de forma atomica: Si algun producto no tiene stock,
     * la transaccion se cancela (rollback) y no se modifica la base de datos.
     * Tras confirmar el commit, publica el evento PedidoCreadoEvent en RabbitMQ.
     */
    @Transactional
    public Pedido crearPedido(PedidoCreateDTO dto, String usuarioAutenticado) {
        Pedido pedido = new Pedido();
        pedido.setOwnerSubject(usuarioAutenticado);
        // Genera un codigo de orden unico y legible (ej: PED-C83E92B1)
        pedido.setNumeroPedido("PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        pedido.setClienteNombre(dto.getClienteNombre());
        pedido.setClienteEmail(dto.getClienteEmail() != null && !dto.getClienteEmail().isBlank()
                ? dto.getClienteEmail()
                : usuarioAutenticado);
        pedido.setFechaCreacion(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        double total = 0.0;

        // Itera cada producto solicitado para validar stock y calcular montos
        for (ItemPedidoDTO itemDto : dto.getItems()) {
            Producto producto = productoRepository.findById(itemDto.getProductoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto con ID " + itemDto.getProductoId() + " no encontrado en el menu"));

            if (producto.getStock() < itemDto.getCantidad()) {
                throw new IllegalStateException("Stock insuficiente para: " + producto.getNombre() + ". Disponibles: " + producto.getStock());
            }

            // Descontar las unidades compradas del stock
            producto.setStock(producto.getStock() - itemDto.getCantidad());
            productoRepository.save(producto);

            // Calcular subtotal de esta linea
            double subtotal = producto.getPrecio() * itemDto.getCantidad();
            ItemPedido item = new ItemPedido(null, pedido, producto.getId(), producto.getNombre(), itemDto.getCantidad(), producto.getPrecio(), subtotal);
            pedido.agregarItem(item);
            total += subtotal;
        }

        pedido.setTotal(total);
        // Guarda el pedido y automaticamente sus items por cascada JPA
        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        publishAfterCommit(cl.duoc.pedidos360.event.PedidoLifecycleEvent.from("PEDIDO_CREADO", pedidoGuardado));

        // Mapear a DTO de evento de negocio desacoplado
        PedidoCreadoEvent event = mapearAEvento(pedidoGuardado);

        // Generar evento para Cola 2 (Notificacion al cliente)
        cl.duoc.pedidos360.event.NotificacionEvent notifEvent = new cl.duoc.pedidos360.event.NotificacionEvent(
                pedidoGuardado.getId(),
                pedidoGuardado.getNumeroPedido(),
                pedidoGuardado.getClienteNombre(),
                pedidoGuardado.getClienteEmail(),
                "CONFIRMACION_PEDIDO",
                "Tu pedido " + pedidoGuardado.getNumeroPedido() + " por $" + pedidoGuardado.getTotal() + " ha sido confirmado exitosamente y esta en cocina.",
                LocalDateTime.now()
        );

        // Generar evento para Cola 3 (Facturacion y Boleta Electronica DTE)
        double totalConIva = pedidoGuardado.getTotal();
        double neto = Math.round(totalConIva / 1.19 * 100.0) / 100.0;
        double iva = Math.round((totalConIva - neto) * 100.0) / 100.0;
        String folio = "DTE-" + pedidoGuardado.getNumeroPedido().replace("PED-", "");

        cl.duoc.pedidos360.event.FacturacionEvent factEvent = new cl.duoc.pedidos360.event.FacturacionEvent(
                pedidoGuardado.getId(),
                pedidoGuardado.getNumeroPedido(),
                folio,
                pedidoGuardado.getClienteNombre(),
                pedidoGuardado.getClienteEmail(),
                neto,
                iva,
                totalConIva,
                LocalDateTime.now()
        );

        // Sincronizacion transaccional: publicar las 3 colas de RabbitMQ solo despues de commit en BD
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    pedidoEventProducer.publicarCicloCompleto(event, notifEvent, factEvent);
                }
            });
        } else {
            pedidoEventProducer.publicarCicloCompleto(event, notifEvent, factEvent);
        }

        return pedidoGuardado;
    }

    // Actualiza el estado del pedido (ej: PENDIENTE -> EN_PREPARACION -> EN_CAMINO -> ENTREGADO)
    @Transactional
    public Pedido actualizarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado con ID: " + id));
        pedido.setEstado(nuevoEstado);
        Pedido actualizado = pedidoRepository.save(pedido);
        publishAfterCommit(cl.duoc.pedidos360.event.PedidoLifecycleEvent.from("ESTADO_ACTUALIZADO", actualizado));
        return actualizado;
    }

    // Elimina una orden (operacion reservada para el rol de administracion)
    @Transactional
    public void eliminarPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado con ID: " + id));
        pedidoRepository.deleteById(id);
        publishAfterCommit(cl.duoc.pedidos360.event.PedidoLifecycleEvent.from("PEDIDO_ELIMINADO", pedido));
    }

    private void publishAfterCommit(cl.duoc.pedidos360.event.PedidoLifecycleEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { domainEvents.publishEvent(event); }
            });
        } else {
            domainEvents.publishEvent(event);
        }
    }

    /**
     * Mapea una entidad JPA Pedido a su DTO de evento de dominio para RabbitMQ
     */
    private PedidoCreadoEvent mapearAEvento(Pedido pedido) {
        List<ItemPedidoEvent> itemsEvent = pedido.getItems() != null
                ? pedido.getItems().stream()
                .map(i -> new ItemPedidoEvent(
                        i.getProductoId(),
                        i.getProductoNombre(),
                        i.getCantidad(),
                        i.getPrecioUnitario(),
                        i.getSubtotal()))
                .toList()
                : List.of();

        return new PedidoCreadoEvent(
                pedido.getId(),
                pedido.getNumeroPedido(),
                pedido.getClienteNombre(),
                pedido.getClienteEmail(),
                pedido.getTotal(),
                pedido.getEstado() != null ? pedido.getEstado().name() : EstadoPedido.PENDIENTE.name(),
                pedido.getFechaCreacion(),
                itemsEvent
        );
    }
}
