import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BrokerStatus,
  QueueInfo,
  ExchangeInfo,
  BindingInfo,
  QueueCreateRequest,
  ExchangeCreateRequest,
  BindingCreateRequest,
} from '../models/rabbitmq.model';

@Injectable({
  providedIn: 'root',
})
export class RabbitMQAdminService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/api/admin/rabbitmq`;

  /**
   * Consulta el estado de conectividad con RabbitMQ
   */
  getBrokerStatus(): Observable<BrokerStatus> {
    return this.http.get<BrokerStatus>(`${this.apiUrl}/status`);
  }

  /**
   * Lista todas las colas administradas
   */
  getQueues(): Observable<QueueInfo[]> {
    return this.http.get<QueueInfo[]>(`${this.apiUrl}/queues`);
  }

  /**
   * Obtiene informacion de una cola especifica
   */
  getQueue(name: string): Observable<QueueInfo> {
    return this.http.get<QueueInfo>(`${this.apiUrl}/queues/${encodeURIComponent(name)}`);
  }

  /**
   * Crea una nueva cola en RabbitMQ
   */
  createQueue(request: QueueCreateRequest): Observable<QueueInfo> {
    return this.http.post<QueueInfo>(`${this.apiUrl}/queues`, request);
  }

  /**
   * Elimina una cola en RabbitMQ
   */
  deleteQueue(name: string, force = false): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/queues/${encodeURIComponent(name)}?force=${force}`
    );
  }

  /**
   * Purga los mensajes de una cola
   */
  purgeQueue(name: string): Observable<{ queue: string; status: string }> {
    return this.http.post<{ queue: string; status: string }>(
      `${this.apiUrl}/queues/${encodeURIComponent(name)}/purge`,
      {}
    );
  }

  /**
   * Lista todos los exchanges administrados
   */
  getExchanges(): Observable<ExchangeInfo[]> {
    return this.http.get<ExchangeInfo[]>(`${this.apiUrl}/exchanges`);
  }

  /**
   * Crea un nuevo exchange
   */
  createExchange(request: ExchangeCreateRequest): Observable<ExchangeInfo> {
    return this.http.post<ExchangeInfo>(`${this.apiUrl}/exchanges`, request);
  }

  /**
   * Elimina un exchange
   */
  deleteExchange(name: string, force = false): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/exchanges/${encodeURIComponent(name)}?force=${force}`
    );
  }

  /**
   * Lista todos los bindings
   */
  getBindings(): Observable<BindingInfo[]> {
    return this.http.get<BindingInfo[]>(`${this.apiUrl}/bindings`);
  }

  /**
   * Crea un nuevo binding entre cola y exchange
   */
  createBinding(request: BindingCreateRequest): Observable<BindingInfo> {
    return this.http.post<BindingInfo>(`${this.apiUrl}/bindings`, request);
  }

  /**
   * Elimina un binding
   */
  deleteBinding(request: BindingCreateRequest): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/bindings`, { body: request });
  }
}
