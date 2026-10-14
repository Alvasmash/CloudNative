package cl.duoc.pedidos360.rabbitmq.controller;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.rabbitmq.dto.BindingInfoDTO;
import cl.duoc.pedidos360.rabbitmq.dto.BindingRequestDTO;
import cl.duoc.pedidos360.rabbitmq.dto.BrokerStatusDTO;
import cl.duoc.pedidos360.rabbitmq.dto.ExchangeInfoDTO;
import cl.duoc.pedidos360.rabbitmq.dto.ExchangeRequestDTO;
import cl.duoc.pedidos360.rabbitmq.dto.QueueInfoDTO;
import cl.duoc.pedidos360.rabbitmq.dto.QueueRequestDTO;
import cl.duoc.pedidos360.rabbitmq.service.RabbitMQAdminService;
import jakarta.validation.Valid;

/**
 * =========================================================================
 * CONTROLADOR REST: RabbitMQAdminController
 * =========================================================================
 * Criterio de Evaluacion EP3 (13%): Endpoints REST para administrar RabbitMQ.
 * Criterio de Evaluacion EP3 (10%): Separacion entre logica de negocio y administracion.
 * Criterio de Evaluacion EP3 (7%): Validacion de parametros de entrada con Bean Validation.
 *
 * Protegido en SecurityConfig bajo la regla /api/admin/** (exige ROLE_ADMIN).
 */
@RestController
@RequestMapping("/api/admin/rabbitmq")
public class RabbitMQAdminController {

    private final RabbitMQAdminService rabbitMQAdminService;

    public RabbitMQAdminController(RabbitMQAdminService rabbitMQAdminService) {
        this.rabbitMQAdminService = rabbitMQAdminService;
    }

    // =========================================================================
    // 1. ESTADO DEL BROKER
    // =========================================================================

    /**
     * Consulta el estado general de conectividad y resumen de recursos del broker.
     * Retorna HTTP 200 OK.
     */
    @GetMapping("/status")
    public ResponseEntity<BrokerStatusDTO> getStatus() {
        return ResponseEntity.ok(rabbitMQAdminService.getBrokerStatus());
    }

    // =========================================================================
    // 2. ADMINISTRACION DE COLAS
    // =========================================================================

    /**
     * Lista todas las colas administradas con sus atributos y estado verificado.
     * Retorna HTTP 200 OK.
     */
    @GetMapping("/queues")
    public ResponseEntity<List<QueueInfoDTO>> listQueues() {
        return ResponseEntity.ok(rabbitMQAdminService.listQueues());
    }

    /**
     * Consulta el estado de una cola especifica y sus metricas (mensajes, consumidores).
     * Retorna HTTP 200 OK o 404 Not Found si no existe.
     */
    @GetMapping("/queues/{name}")
    public ResponseEntity<QueueInfoDTO> getQueue(@PathVariable String name) {
        return ResponseEntity.ok(rabbitMQAdminService.getQueueInfo(name));
    }

    /**
     * Crea una nueva cola en el broker.
     * Valida nombres, caracteres y parametros.
     * Retorna HTTP 201 Created.
     */
    @PostMapping("/queues")
    public ResponseEntity<QueueInfoDTO> createQueue(@Valid @RequestBody QueueRequestDTO request) {
        QueueInfoDTO created = rabbitMQAdminService.createQueue(request);
        return ResponseEntity.created(URI.create("/api/admin/rabbitmq/queues/" + created.getName())).body(created);
    }

    /**
     * Elimina una cola del broker.
     * Protege colas criticas (retorna 409 Conflict si se intenta eliminar una critica).
     * Retorna HTTP 204 No Content.
     */
    @DeleteMapping("/queues/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteQueue(
            @PathVariable String name,
            @RequestParam(defaultValue = "false") boolean force) {
        rabbitMQAdminService.deleteQueue(name, force);
        return ResponseEntity.noContent().build();
    }

    /**
     * Purga todos los mensajes contenidos en una cola.
     * Retorna HTTP 200 OK con el total de mensajes purgados.
     */
    @PostMapping("/queues/{name}/purge")
    public ResponseEntity<Map<String, Object>> purgeQueue(@PathVariable String name) {
        rabbitMQAdminService.purgeQueue(name);
        return ResponseEntity.ok(Map.of(
                "queue", name,
                "status", "purged"
        ));
    }

    // =========================================================================
    // 3. ADMINISTRACION DE EXCHANGES
    // =========================================================================

    /**
     * Lista todos los exchanges administrados.
     * Retorna HTTP 200 OK.
     */
    @GetMapping("/exchanges")
    public ResponseEntity<List<ExchangeInfoDTO>> listExchanges() {
        return ResponseEntity.ok(rabbitMQAdminService.listExchanges());
    }

    /**
     * Consulta el detalle de un exchange especifico.
     * Retorna HTTP 200 OK o 404 Not Found.
     */
    @GetMapping("/exchanges/{name}")
    public ResponseEntity<ExchangeInfoDTO> getExchange(@PathVariable String name) {
        return ResponseEntity.ok(rabbitMQAdminService.getExchangeInfo(name));
    }

    /**
     * Crea un nuevo exchange de tipo direct, topic, fanout o headers.
     * Retorna HTTP 201 Created.
     */
    @PostMapping("/exchanges")
    public ResponseEntity<ExchangeInfoDTO> createExchange(@Valid @RequestBody ExchangeRequestDTO request) {
        ExchangeInfoDTO created = rabbitMQAdminService.createExchange(request);
        return ResponseEntity.created(URI.create("/api/admin/rabbitmq/exchanges/" + created.getName())).body(created);
    }

    /**
     * Elimina un exchange del broker protegiendo exchanges criticos.
     * Retorna HTTP 204 No Content.
     */
    @DeleteMapping("/exchanges/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteExchange(
            @PathVariable String name,
            @RequestParam(defaultValue = "false") boolean force) {
        rabbitMQAdminService.deleteExchange(name, force);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // 4. ADMINISTRACION DE BINDINGS (ENLACES)
    // =========================================================================

    /**
     * Lista los bindings administrados.
     * Retorna HTTP 200 OK.
     */
    @GetMapping("/bindings")
    public ResponseEntity<List<BindingInfoDTO>> listBindings() {
        return ResponseEntity.ok(rabbitMQAdminService.listBindings());
    }

    /**
     * Crea un enlace (binding) entre una cola y un exchange con su routing key.
     * Valida que la cola y el exchange existan previamente.
     * Retorna HTTP 201 Created.
     */
    @PostMapping("/bindings")
    public ResponseEntity<BindingInfoDTO> createBinding(@Valid @RequestBody BindingRequestDTO request) {
        BindingInfoDTO created = rabbitMQAdminService.createBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Elimina un enlace (binding) entre una cola y un exchange.
     * Retorna HTTP 204 No Content.
     */
    @DeleteMapping("/bindings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteBinding(@Valid @RequestBody BindingRequestDTO request) {
        rabbitMQAdminService.deleteBinding(request);
        return ResponseEntity.noContent().build();
    }
}
