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
    /** Exchange principal de tipo Topic para distribucion desacoplada de eventos de pedidos */
    public static final String EXCHANGE_PEDIDOS = "pedidos.exchange";

    /** Exchange punto a punto de tipo Direct para comandos y operaciones sincronizables */
    public static final String EXCHANGE_DIRECT = "pedidos.direct.exchange";

    /** Exchange directo para enrutar mensajes a la cola de reintentos con TTL */
    public static final String EXCHANGE_RETRY = "pedidos.retry.exchange";

    /** Dead Letter Exchange (DLX) directo para derivar mensajes no procesables hacia su DLQ */
    public static final String EXCHANGE_DLX = "pedidos.dlx";

    // =========================================================================
    // 3 COLAS PRINCIPALES Y SUS RESPECTIVAS DEAD LETTER QUEUES (DLQ)
    // =========================================================================
    /** COLA 1: Procesamiento principal y preparacion en cocina de pedidos creados */
    public static final String QUEUE_PEDIDO_CREADO = "pedidos.pedido-creado.queue";

    /** DLQ 1: Dead Letter Queue para pedidos fallidos que superan reintentos o son invalidos */
    public static final String QUEUE_DLQ = "pedidos.pedido-creado.dlq";

    /** COLA 2: Notificaciones asincronas al cliente (Correo, SMS o Push con estado del pedido) */
    public static final String QUEUE_NOTIFICACION = "pedidos.notificacion.queue";

    /** DLQ 2: Dead Letter Queue para notificaciones fallidas (correo invalido, servicio externo caido) */
    public static final String QUEUE_NOTIFICACION_DLQ = "pedidos.notificacion.dlq";

    /** COLA 3: Facturacion y emision de Documentos Tributarios Electronicos (Boletas DTE) */
    public static final String QUEUE_FACTURACION = "pedidos.facturacion.queue";

    /** DLQ 3: Dead Letter Queue para errores en calculo de impuestos o emision de boletas */
    public static final String QUEUE_FACTURACION_DLQ = "pedidos.facturacion.dlq";

    /** Cola de reintentos temporizada (TTL) con dead-letter de vuelta al exchange principal */
    public static final String QUEUE_RETRY = "pedidos.pedido-creado.retry.queue";

    /** Cola para auditoria y trazabilidad global de eventos de negocio */
    public static final String QUEUE_AUDITORIA = "pedidos.auditoria.queue";

    // =========================================================================
    // ROUTING KEYS
    // =========================================================================
    /** Routing key emitida cuando se crea un pedido para preparacion */
    public static final String RK_PEDIDO_CREADO = "pedido.creado";

    /** Routing key emitida para notificar al cliente del estado de su pedido */
    public static final String RK_NOTIFICACION = "pedido.notificacion";

    /** Routing key emitida para generar la boleta fiscal del pedido */
    public static final String RK_FACTURACION = "pedido.facturacion";

    /** Patron comodin de topic para capturar todo el ciclo de vida: "pedido.*" */
    public static final String RK_PEDIDO_ALL = "pedido.*";

    /** Routing key utilizada para enrutar hacia la cola de reintentos con TTL */
    public static final String RK_RETRY = "pedido.reintento";

    /** Routing key utilizada para enrutar hacia la DLQ de pedidos creados */
    public static final String RK_DLQ = "pedido.dlq";

    /** Routing key utilizada para enrutar hacia la DLQ de notificaciones */
    public static final String RK_NOTIFICACION_DLQ = "notificacion.dlq";

    /** Routing key utilizada para enrutar hacia la DLQ de facturacion */
    public static final String RK_FACTURACION_DLQ = "facturacion.dlq";

    /** Routing key directa para comandos punto a punto */
    public static final String RK_DIRECT_COMMAND = "pedido.direct.cmd";

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

    /** Header con el nombre de la cola de origen */
    public static final String HEADER_X_ORIGINAL_QUEUE = "x-original-queue";

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
            QUEUE_DLQ,
            QUEUE_NOTIFICACION,
            QUEUE_NOTIFICACION_DLQ,
            QUEUE_FACTURACION,
            QUEUE_FACTURACION_DLQ,
            QUEUE_RETRY,
            QUEUE_AUDITORIA
    ));

    /** Conjunto inmutable de exchanges criticos cuya eliminacion por API esta prohibida */
    public static final Set<String> CRITICAL_EXCHANGES = Collections.unmodifiableSet(Set.of(
            EXCHANGE_PEDIDOS,
            EXCHANGE_DIRECT,
            EXCHANGE_RETRY,
            EXCHANGE_DLX
    ));
}
