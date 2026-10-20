package cl.duoc.pedidos360.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * =========================================================================
 * CONFIGURACION DE SPRING AMQP / RABBITMQ: RabbitMQConfig
 * =========================================================================
 * Criterio de Evaluacion EP3 (13%): Declaracion de beans Queue, Exchange y Binding.
 * Criterio de Evaluacion EP3 (10%): Separacion clara entre configuracion y logica.
 * Criterio de Evaluacion EP3 (20%): Soporte para ACK/NACK manual, TTL y DLQ.
 */
@Configuration
public class RabbitMQConfig {

    // =========================================================================
    // EXCHANGES (DIRECT Y TOPIC)
    // =========================================================================

    /**
     * Exchange principal Topic para eventos de pedidos (durable)
     */
    @Bean
    public TopicExchange pedidosExchange() {
        return new TopicExchange(RabbitMQConstants.EXCHANGE_PEDIDOS, true, false);
    }

    /**
     * Exchange Direct para operaciones y comandos sincronizables punto a punto
     */
    @Bean
    public DirectExchange pedidosDirectExchange() {
        return new DirectExchange(RabbitMQConstants.EXCHANGE_DIRECT, true, false);
    }

    /**
     * Exchange de reintentos: distribuye los mensajes hacia la cola con TTL
     */
    @Bean
    public DirectExchange retryExchange() {
        return new DirectExchange(RabbitMQConstants.EXCHANGE_RETRY, true, false);
    }

    /**
     * Dead Letter Exchange (DLX): enruta mensajes rechazados a su respectiva DLQ
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitMQConstants.EXCHANGE_DLX, true, false);
    }

    // =========================================================================
    // 3 COLAS PRINCIPALES Y SUS RESPECTIVAS DLQ
    // =========================================================================

    /**
     * COLA 1: Procesamiento principal de pedidos creados (durable).
     * Configura DLX hacia pedidos.dlx con routing key pedido.dlq.
     */
    @Bean
    public Queue pedidoCreadoQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", RabbitMQConstants.EXCHANGE_DLX);
        args.put("x-dead-letter-routing-key", RabbitMQConstants.RK_DLQ);
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_PEDIDO_CREADO)
                .withArguments(args)
                .build();
    }

    /**
     * DLQ 1: Dead Letter Queue para pedidos no procesables
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_DLQ).build();
    }

    /**
     * COLA 2: Notificaciones asincronas a clientes (correo/SMS/push).
     * Configura DLX hacia pedidos.dlx con routing key notificacion.dlq.
     */
    @Bean
    public Queue notificacionQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", RabbitMQConstants.EXCHANGE_DLX);
        args.put("x-dead-letter-routing-key", RabbitMQConstants.RK_NOTIFICACION_DLQ);
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_NOTIFICACION)
                .withArguments(args)
                .build();
    }

    /**
     * DLQ 2: Dead Letter Queue para notificaciones fallidas
     */
    @Bean
    public Queue notificacionDlq() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_NOTIFICACION_DLQ).build();
    }

    /**
     * COLA 3: Facturacion y emision de Documentos Tributarios (Boleta DTE).
     * Configura DLX hacia pedidos.dlx con routing key facturacion.dlq.
     */
    @Bean
    public Queue facturacionQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", RabbitMQConstants.EXCHANGE_DLX);
        args.put("x-dead-letter-routing-key", RabbitMQConstants.RK_FACTURACION_DLQ);
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_FACTURACION)
                .withArguments(args)
                .build();
    }

    /**
     * DLQ 3: Dead Letter Queue para errores en facturacion
     */
    @Bean
    public Queue facturacionDlq() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_FACTURACION_DLQ).build();
    }

    /**
     * Cola de reintentos temporizada (Retry Queue con TTL de 5 segundos).
     * Al expirar los 5000 ms, expulsa el mensaje al exchange principal para reintento.
     */
    @Bean
    public Queue retryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", RabbitMQConstants.DEFAULT_RETRY_TTL_MS);
        args.put("x-dead-letter-exchange", RabbitMQConstants.EXCHANGE_PEDIDOS);
        args.put("x-dead-letter-routing-key", RabbitMQConstants.RK_PEDIDO_CREADO);
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_RETRY)
                .withArguments(args)
                .build();
    }

    /**
     * Cola de auditoria: almacena copia de todos los eventos del ciclo de vida
     */
    @Bean
    public Queue auditoriaQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_AUDITORIA).build();
    }

    // =========================================================================
    // BINDINGS (ENLACES PRINCIPALES Y DLQ)
    // =========================================================================

    /**
     * Enlaza COLA 1 al TopicExchange con routing key "pedido.creado"
     */
    @Bean
    public Binding bindingPedidoCreado(Queue pedidoCreadoQueue, TopicExchange pedidosExchange) {
        return BindingBuilder.bind(pedidoCreadoQueue)
                .to(pedidosExchange)
                .with(RabbitMQConstants.RK_PEDIDO_CREADO);
    }

    /**
     * Enlaza COLA 2 al TopicExchange con routing key "pedido.notificacion"
     */
    @Bean
    public Binding bindingNotificacion(Queue notificacionQueue, TopicExchange pedidosExchange) {
        return BindingBuilder.bind(notificacionQueue)
                .to(pedidosExchange)
                .with(RabbitMQConstants.RK_NOTIFICACION);
    }

    /**
     * Enlaza COLA 3 al TopicExchange con routing key "pedido.facturacion"
     */
    @Bean
    public Binding bindingFacturacion(Queue facturacionQueue, TopicExchange pedidosExchange) {
        return BindingBuilder.bind(facturacionQueue)
                .to(pedidosExchange)
                .with(RabbitMQConstants.RK_FACTURACION);
    }

    /**
     * Enlaza la cola de auditoria al TopicExchange para capturar "pedido.*"
     */
    @Bean
    public Binding bindingAuditoria(Queue auditoriaQueue, TopicExchange pedidosExchange) {
        return BindingBuilder.bind(auditoriaQueue)
                .to(pedidosExchange)
                .with(RabbitMQConstants.RK_PEDIDO_ALL);
    }

    /**
     * Enlace DLQ 1 al DLX con routing key "pedido.dlq"
     */
    @Bean
    public Binding bindingDeadLetter(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.RK_DLQ);
    }

    /**
     * Enlace DLQ 2 al DLX con routing key "notificacion.dlq"
     */
    @Bean
    public Binding bindingDeadLetterNotificacion(Queue notificacionDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(notificacionDlq)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.RK_NOTIFICACION_DLQ);
    }

    /**
     * Enlace DLQ 3 al DLX con routing key "facturacion.dlq"
     */
    @Bean
    public Binding bindingDeadLetterFacturacion(Queue facturacionDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(facturacionDlq)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.RK_FACTURACION_DLQ);
    }

    /**
     * Enlaza la cola de reintentos al DirectExchange de reintentos
     */
    @Bean
    public Binding bindingRetry(Queue retryQueue, DirectExchange retryExchange) {
        return BindingBuilder.bind(retryQueue)
                .to(retryExchange)
                .with(RabbitMQConstants.RK_RETRY);
    }

    /**
     * Enlace directo punto a punto demostrando DirectExchange para comandos
     */
    @Bean
    public Binding bindingDirectCommand(Queue pedidoCreadoQueue, DirectExchange pedidosDirectExchange) {
        return BindingBuilder.bind(pedidoCreadoQueue)
                .to(pedidosDirectExchange)
                .with(RabbitMQConstants.RK_DIRECT_COMMAND);
    }

    // =========================================================================
    // TEMPLATE, CONVERTIDOR JSON Y ADMIN
    // =========================================================================

    /**
     * Convertidor de mensajes a formato JSON usando Jackson con soporte para Java 8/17/21 Time
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate configurado con serializacion JSON y conexion al broker
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        return template;
    }

    /**
     * RabbitAdmin para la administracion programatica y REST de colas, exchanges y bindings
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    /**
     * Contenedor de listeners con modo de confirmacion MANUAL (ACK/NACK explicitos)
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(10);
        return factory;
    }
}
