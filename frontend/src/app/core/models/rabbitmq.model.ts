/**
 * Modelos de datos para la administracion de RabbitMQ desde Angular
 */

export interface BrokerStatus {
  status: string;
  host: string;
  port: number;
  virtualHost: string;
  totalQueues: number;
  totalExchanges: number;
  totalBindings: number;
  timestamp: string;
}

export interface QueueInfo {
  name: string;
  durable: boolean;
  exclusive: boolean;
  autoDelete: boolean;
  isCritical: boolean;
  verifiedInBroker: boolean;
  messageCount: number;
  consumerCount: number;
  arguments: Record<string, any>;
}

export interface ExchangeInfo {
  name: string;
  type: string;
  durable: boolean;
  autoDelete: boolean;
  isCritical: boolean;
  verifiedInBroker: boolean;
  arguments: Record<string, any>;
}

export interface BindingInfo {
  queueName: string;
  exchangeName: string;
  routingKey: string;
  arguments: Record<string, any>;
}

export interface QueueCreateRequest {
  name: string;
  durable?: boolean;
  exclusive?: boolean;
  autoDelete?: boolean;
  arguments?: Record<string, any>;
}

export interface ExchangeCreateRequest {
  name: string;
  type: 'direct' | 'topic' | 'fanout' | 'headers';
  durable?: boolean;
  autoDelete?: boolean;
  arguments?: Record<string, any>;
}

export interface BindingCreateRequest {
  queueName: string;
  exchangeName: string;
  routingKey: string;
  arguments?: Record<string, any>;
}
