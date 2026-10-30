import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpContext } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { OPTIONAL_SERVER_FEATURE } from '../../core/interceptors/optional-feature.context';

interface KafkaEvent {
  event: { eventId: string; type: string; pedidoId: number; numeroPedido: string; estado: string; occurredAt: string };
  partition: number;
  offset: number;
}
interface KafkaStatus { status: 'CONNECTED' | 'DISCONNECTED' | 'DISABLED'; topic: string; events: KafkaEvent[]; retentionDays?: number; }

@Component({
  selector: 'app-kafka-monitor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="kafka-panel" aria-labelledby="kafka-title">
      <div class="kafka-heading">
        <div><span class="eyebrow">ACTIVIDAD DE PEDIDOS</span><h2 id="kafka-title">Eventos en Kafka</h2></div>
        <button type="button" (click)="refresh()" [disabled]="loading()">{{ loading() ? 'Consultando…' : unavailable() ? 'Comprobar de nuevo' : 'Actualizar eventos' }}</button>
      </div>
      @if (unavailable()) {
        <p class="notice" role="status">Este servidor no ofrece el panel de Kafka. Para consultar sus eventos hay que actualizar el backend publicado con la integración de Kafka.</p>
      } @else if (error()) {
        <p class="notice" role="alert">No pudimos consultar Kafka. Puedes reintentar con Actualizar eventos.</p>
      } @else { @if (data(); as stream) {
        <div class="kafka-summary">
          <span class="connection" [class.connected]="stream.status === 'CONNECTED'">
            <span aria-hidden="true">●</span> {{ stream.status === 'CONNECTED' ? 'Broker conectado' : stream.status === 'DISABLED' ? 'Kafka desactivado' : 'Broker sin conexión' }}
          </span>
          @if (stream.topic) { <code>{{ stream.topic }}</code><span class="muted">Retención: {{ stream.retentionDays }} días</span> }
        </div>
        @if (stream.status === 'DISCONNECTED') { <p class="notice">No se pudo conectar con el broker de Kafka de este servidor.</p> }
        @if (stream.events.length) {
          <div class="event-scroll"><table>
            <caption class="sr-only">Últimos eventos recibidos por el consumidor de auditoría</caption>
            <thead><tr><th>Evento</th><th>Pedido</th><th>Estado</th><th>Hora</th><th>Partición / offset</th></tr></thead>
            <tbody>@for (item of stream.events; track item.event.eventId + ':' + item.partition + ':' + item.offset) {
              <tr><td>{{ eventName(item.event.type) }}</td><td><strong>{{ item.event.numeroPedido }}</strong></td><td>{{ item.event.estado }}</td><td>{{ item.event.occurredAt | date:'dd/MM HH:mm:ss' }}</td><td><code>{{ item.partition }} / {{ item.offset }}</code></td></tr>
            }</tbody>
          </table></div>
        } @else if (stream.status === 'CONNECTED') {
          <p class="empty">Aún no hay eventos recibidos en esta sesión del backend. Crea un pedido o cambia su estado y actualiza este panel.</p>
        }
        <p class="footnote">Últimos 50 eventos recibidos por este backend desde su inicio. Kafka conserva el historial del tópico.</p>
      } @else { <p class="muted" role="status">Consultando el registro de eventos…</p> }
    } </section>
  `,
  styles: [`
    .kafka-panel{margin-top:28px;background:white;border:1px solid #e2e8dc;border-radius:18px;padding:24px}
    .kafka-heading{display:flex;justify-content:space-between;align-items:center;gap:16px}.eyebrow{font-size:10px;font-weight:700;letter-spacing:.12em;color:#64735c}h2{font-size:23px;letter-spacing:-.035em;margin:6px 0 0}
    button{border:1px solid #dce5d4;border-radius:9px;background:#edf5e1;padding:11px 16px;font-size:12px;font-weight:600;color:#234b36;cursor:pointer}button:disabled{opacity:.6;cursor:wait}
    .kafka-summary{display:flex;align-items:center;flex-wrap:wrap;gap:14px;margin:22px 0;font-size:12px}.connection{background:#fff3dd;color:#855b17;border-radius:30px;padding:7px 11px;display:flex;gap:7px}.connection.connected{background:#eaf5dd;color:#315b24}code{font-size:11px;overflow-wrap:anywhere}.muted,.footnote{color:#64735c;font-size:12px}.footnote{margin-top:18px;font-size:11px;line-height:1.6}
    .notice{background:#fff6e6;color:#73521b;padding:13px;border-radius:9px;font-size:13px;margin-top:14px}.empty{color:#64735c;font-size:13px;padding:20px 0;line-height:1.7}
    .event-scroll{overflow-x:auto}table{width:100%;border-collapse:collapse;text-align:left;white-space:nowrap}th{font-size:10px;text-transform:uppercase;letter-spacing:.04em;color:#64735c;background:#f5f8ef}td,th{padding:13px 12px;border-bottom:1px solid #edf1e6}td{font-size:12px;color:#3e5144}.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}
    @media(max-width:600px){.kafka-panel{padding:18px}.kafka-heading{align-items:start;flex-direction:column}.kafka-summary{gap:10px}h2{font-size:21px}}
  `],
})
export class KafkaMonitorComponent implements OnInit {
  private readonly http = inject(HttpClient);
  readonly data = signal<KafkaStatus | null>(null);
  readonly loading = signal(false);
  readonly error = signal(false);
  readonly unavailable = signal(false);
  ngOnInit(): void { this.refresh(); }
  refresh(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set(false);
    this.unavailable.set(false);
    this.http.get<KafkaStatus>(`${environment.apiUrl}/api/admin/kafka`, {
      context: new HttpContext().set(OPTIONAL_SERVER_FEATURE, true),
    }).subscribe({
      next: value => { this.data.set(value); this.loading.set(false); },
      error: error => {
        this.data.set(null);
        this.unavailable.set(error.status === 404);
        this.error.set(error.status !== 404);
        this.loading.set(false);
      },
    });
  }
  eventName(type: string): string {
    return ({ PEDIDO_CREADO: 'Pedido creado', ESTADO_ACTUALIZADO: 'Estado actualizado', PEDIDO_ELIMINADO: 'Pedido eliminado' } as Record<string, string>)[type] || type;
  }
}

