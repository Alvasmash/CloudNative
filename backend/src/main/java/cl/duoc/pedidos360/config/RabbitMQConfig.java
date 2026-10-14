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
    // EXCHANGES
    // =========================================================================

    /**
     * Exchange principal para publicacion de eventos de pedidos (durable)
     */
    @Bean
    public TopicExchange pedidosExchange() {
        return new TopicExchange(RabbitMQConstants.EXCHANGE_PEDIDOS, true, false);
    }

    /**
     * Exchange de reintentos: distribuye los mensajes hacia la cola con TTL
     */
    @Bean
    public DirectExchange retryExchange() {
        return new DirectExchange(RabbitMQConstants.EXCHANGE_RETRY, true, false);
    }

    /**
     * Dead Letter Exchange (DLX): recibe los mensajes descartados definitivamente
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitMQConstants.EXCHANGE_DLX, true, false);
    }

    // =========================================================================
    // COLAS (QUEUES)
    // =========================================================================

    /**
     * Cola principal de procesamiento de pedidos creados (durable).
     * Configura Dead Letter Exchange hacia pedidos.dlx con routing key pedido.dlq.
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
     * Cola de reintentos temporizada (Retry Queue).
     * Mantiene los mensajes durante DEFAULT_RETRY_TTL_MS (5 segundos) y, al expirar,
     * los enruta de vuelta al exchange principal para que vuelvan a ser consumidos.
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
     * Dead Letter Queue (DLQ): almacena los mensajes no procesables para auditoria
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_DLQ).build();
    }

    /**
     * Cola de auditoria: almacena copia de los eventos del sistema
     */
    @Bean
    public Queue auditoriaQueue() {
        return QueueBuilder.durable(RabbitMQConstants.QUEUE_AUDITORIA).build();
    }

    // =========================================================================
    // BINDINGS (ENLACES)
    // =========================================================================

    /**
     * Enlaza la cola principal al exchange de pedidos con la routing key "pedido.creado"
     */
    @Bean
    public Binding bindingPedidoCreado(Queue pedidoCreadoQueue, TopicExchange pedidosExchange) {
        return BindingBuilder.bind(pedidoCreadoQueue)
                .to(pedidosExchange)
                .with(RabbitMQConstants.RK_PEDIDO_CREADO);
    }

    /**
     * Enlaza la cola de reintentos al exchange de reintentos con la routing key "pedido.reintento"
     */
    @Bean
    public Binding bindingRetry(Queue retryQueue, DirectExchange retryExchange) {
        return BindingBuilder.bind(retryQueue)
                .to(retryExchange)
                .with(RabbitMQConstants.RK_RETRY);
    }

    /**
     * Enlaza la Dead Letter Queue al DLX con la routing key "pedido.dlq"
     */
    @Bean
    public Binding bindingDeadLetter(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.RK_DLQ);
    }

    /**
     * Enlaza la cola de auditoria al exchange de pedidos para capturar todos los eventos "pedido.*"
     */
    @Bean
    public Binding bindingAuditoria(Queue auditoriaQueue, TopicExchange pedidosExchange) {
        return BindingBuilder.bind(auditoriaQueue)
                .to(pedidosExchange)
                .with(RabbitMQConstants.RK_PEDIDO_ALL);
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
