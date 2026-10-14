package cl.duoc.pedidos360.rabbitmq.service;

import java.util.List;

import cl.duoc.pedidos360.rabbitmq.dto.BindingInfoDTO;
import cl.duoc.pedidos360.rabbitmq.dto.BindingRequestDTO;
import cl.duoc.pedidos360.rabbitmq.dto.BrokerStatusDTO;
import cl.duoc.pedidos360.rabbitmq.dto.ExchangeInfoDTO;
import cl.duoc.pedidos360.rabbitmq.dto.ExchangeRequestDTO;
import cl.duoc.pedidos360.rabbitmq.dto.QueueInfoDTO;
import cl.duoc.pedidos360.rabbitmq.dto.QueueRequestDTO;

/**
 * =========================================================================
 * SERVICIO DEDICADO: RabbitMQAdminService
 * =========================================================================
 * Criterio de Evaluacion EP3 (10%): Servicio dedicado para administrar
 * colas, exchanges y bindings contra el broker mediante RabbitAdmin.
 */
public interface RabbitMQAdminService {

    /**
     * Consulta el estado de conectividad con el broker RabbitMQ y resume recursos
     */
    BrokerStatusDTO getBrokerStatus();

    /**
     * Lista todas las colas administradas distinguiendo su verificacion en el broker
     */
    List<QueueInfoDTO> listQueues();

    /**
     * Obtiene el detalle y metricas en vivo de una cola especifica
     */
    QueueInfoDTO getQueueInfo(String queueName);

    /**
     * Declara y crea una nueva cola durable o temporal en el broker
     */
    QueueInfoDTO createQueue(QueueRequestDTO request);

    /**
     * Elimina una cola del broker protegiendo los recursos criticos del sistema
     */
    void deleteQueue(String queueName, boolean force);

    /**
     * Lista todos los exchanges administrados
     */
    List<ExchangeInfoDTO> listExchanges();

    /**
     * Obtiene el detalle de un exchange especifico
     */
    ExchangeInfoDTO getExchangeInfo(String exchangeName);

    /**
     * Declara y crea un nuevo exchange (direct, topic, fanout o headers)
     */
    ExchangeInfoDTO createExchange(ExchangeRequestDTO request);

    /**
     * Elimina un exchange del broker protegiendo los recursos criticos del sistema
     */
    void deleteExchange(String exchangeName, boolean force);

    /**
     * Lista los enlaces (bindings) administrados
     */
    List<BindingInfoDTO> listBindings();

    /**
     * Enlaza una cola a un exchange con una routing key validando existencia previa
     */
    BindingInfoDTO createBinding(BindingRequestDTO request);

    /**
     * Elimina un enlace (binding) existente
     */
    void deleteBinding(BindingRequestDTO request);

    /**
     * Purga todos los mensajes encolados en una cola especifica
     */
    void purgeQueue(String queueName);
}
