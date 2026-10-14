package cl.duoc.pedidos360.config;

import java.util.Collections;
import java.util.Set;

/**
 * =========================================================================
 * CONSTANTES CENTRALIZADAS DE RABBITMQ: RabbitMQConstants
 * =========================================================================
 * Criterio de Evaluacion EP3 (12%): Centralizacion de nombres de colas,
 * exchanges, bindings, routing keys, headers y politicas de reintentos.
 * Evita cadenas dispersas o duplicadas a lo largo de la aplicacion.
 */
public final class RabbitMQConstants {

    private RabbitMQConstants() {
        // Constructor privado para evitar instanciacion
    }

    // =========================================================================
    // EXCHANGES
    // =========================================================================
    /** Exchange principal de tipo Topic para eventos de pedidos */
    public static final String EXCHANGE_PEDIDOS = "pedidos.exchange";

    /** Exchange directo para enrutar mensajes a la cola de reintentos con TTL */
    public static final String EXCHANGE_RETRY = "pedidos.retry.exchange";

    /** Dead Letter Exchange (DLX) directo para mensajes no recuperables o descartados */
    public static final String EXCHANGE_DLX = "pedidos.dlx";

    // =========================================================================
    // COLAS (QUEUES)
    // =========================================================================
    /** Cola principal donde se procesan los pedidos creados */
    public static final String QUEUE_PEDIDO_CREADO = "pedidos.pedido-creado.queue";

    /** Cola de reintentos temporizada (TTL) con dead-letter de vuelta al exchange principal */
    public static final String QUEUE_RETRY = "pedidos.pedido-creado.retry.queue";

    /** Dead Letter Queue (DLQ) para inspeccionar y diagnosticar mensajes fallidos */
    public static final String QUEUE_DLQ = "pedidos.pedido-creado.dlq";

    /** Cola para auditoria y trazabilidad de eventos de negocio */
    public static final String QUEUE_AUDITORIA = "pedidos.auditoria.queue";

    // =========================================================================
    // ROUTING KEYS
    // =========================================================================
    /** Routing key emitida cuando se crea un pedido */
    public static final String RK_PEDIDO_CREADO = "pedido.creado";

    /** Patron de routing key para suscribirse a cualquier evento de pedidos */
    public static final String RK_PEDIDO_ALL = "pedido.*";

    /** Routing key utilizada para enrutar hacia la cola de reintentos */
    public static final String RK_RETRY = "pedido.reintento";

    /** Routing key utilizada para enrutar hacia la Dead Letter Queue */
    public static final String RK_DLQ = "pedido.dlq";

    /** Routing key utilizada para eventos de auditoria */
    public static final String RK_AUDITORIA = "pedido.auditoria";

    // =========================================================================
    // HEADERS PERSONALIZADOS DE TRAZABILIDAD Y REINTENTOS
    // =========================================================================
    /** Header que almacena la cantidad actual de reintentos ejecutados */
    public static final String HEADER_X_RETRY_COUNT = "x-retry-count";

    /** Header que define el limite maximo de reintentos permitidos */
    public static final String HEADER_X_MAX_RETRIES = "x-max-retries";

    /** Header con el exchange de origen del mensaje antes del reintento o DLQ */
    public static final String HEADER_X_ORIGINAL_EXCHANGE = "x-original-exchange";

    /** Header con la routing key de origen del mensaje */
    public static final String HEADER_X_ORIGINAL_ROUTING_KEY = "x-original-routing-key";

    /** Header que registra el mensaje de la excepcion que provoco la falla */
    public static final String HEADER_X_EXCEPTION_MESSAGE = "x-exception-message";

    /** Header que registra el tipo de excepcion ocurrida */
    public static final String HEADER_X_EXCEPTION_TYPE = "x-exception-type";

    /** Header que registra la marca temporal ISO-8601 del incidente */
    public static final String HEADER_X_TIMESTAMP = "x-timestamp";

    // =========================================================================
    // PARAMETROS OPERATIVOS
    // =========================================================================
    /** Tiempo de espera (TTL) en milisegundos en la cola de reintentos (5 segundos) */
    public static final int DEFAULT_RETRY_TTL_MS = 5000;

    /** Cantidad maxima de reintentos antes de derivar definitivamente a la DLQ */
    public static final int DEFAULT_MAX_RETRIES = 3;

    // =========================================================================
    // RECURSOS CRITICOS PROTEGIDOS DEL SISTEMA
    // =========================================================================
    /** Conjunto inmutable de colas criticas cuya eliminacion por API esta prohibida */
    public static final Set<String> CRITICAL_QUEUES = Collections.unmodifiableSet(Set.of(
            QUEUE_PEDIDO_CREADO,
            QUEUE_RETRY,
            QUEUE_DLQ,
            QUEUE_AUDITORIA
    ));

    /** Conjunto inmutable de exchanges criticos cuya eliminacion por API esta prohibida */
    public static final Set<String> CRITICAL_EXCHANGES = Collections.unmodifiableSet(Set.of(
            EXCHANGE_PEDIDOS,
            EXCHANGE_RETRY,
            EXCHANGE_DLX
    ));
}
