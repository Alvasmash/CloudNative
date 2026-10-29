import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Pedido } from '../models/pedido.model';
import { PedidoCreateDTO } from '../models/pedido-create.model';
import { ToastService } from './toast.service';
import { AuthService } from '../auth/auth.service';
const GUEST_KEY = 'pedidos360_guest_access';
const GUEST_IDS = 'pedidos360_guest_order_ids';
@Injectable({providedIn:'root'})
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);
  private readonly apiUrl = `${environment.apiUrl}/api/pedidos`;
  private readonly guestUrl = `${environment.apiUrl}/public/guest/pedidos`;

  private guestToken(create = false): string | null {
    const stored = sessionStorage.getItem(GUEST_KEY);
    if (stored && /^[A-Za-z0-9_-]{43}$/.test(stored)) return stored;
    if (!create) return null;
    const bytes = crypto.getRandomValues(new Uint8Array(32));
    const token = btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    sessionStorage.setItem(GUEST_KEY, token);
    return token;
  }
  private guestIds(): number[] {
    try { const ids = JSON.parse(sessionStorage.getItem(GUEST_IDS) || '[]'); return Array.isArray(ids) ? ids.filter(Number.isInteger) : []; }
    catch { return []; }
  }
  private rememberGuestOrders(orders: Pedido[]): void {
    sessionStorage.setItem(GUEST_IDS, JSON.stringify([...new Set([...this.guestIds(), ...orders.map(order => order.id)])]));
  }
  private guestOrders(): Observable<Pedido[]> {
    const token = this.guestToken();
    return token ? this.http.get<Pedido[]>(this.guestUrl, {headers: {'X-Guest-Token': token}})
      .pipe(tap(orders => this.rememberGuestOrders(orders))) : of([]);
  }
  crearPedido(dto: PedidoCreateDTO): Observable<Pedido> {
    const request = this.auth.isAuthenticated()
      ? this.http.post<Pedido>(this.apiUrl, dto)
      : this.http.post<Pedido>(this.guestUrl, dto, {headers: {'X-Guest-Token': this.guestToken(true)!}})
        .pipe(tap(order => this.rememberGuestOrders([order])));
    return request.pipe(tap(order => this.toast.success(`Pedido creado con éxito: ${order.numeroPedido}`)));
  }
  getMisPedidos(): Observable<Pedido[]> {
    if (!this.auth.isAuthenticated()) return this.guestOrders();
    return forkJoin([this.http.get<Pedido[]>(this.apiUrl), this.guestOrders()]).pipe(
      map(([owned, guest]) => [...new Map([...owned, ...guest].map(order => [order.id, order])).values()]
        .sort((a, b) => Date.parse(b.fechaCreacion) - Date.parse(a.fechaCreacion))));
  }
  getPedidoById(id: number): Observable<Pedido | null> {
    const token = this.guestToken();
    if (token && (!this.auth.isAuthenticated() || this.guestIds().includes(id))) {
      return this.http.get<Pedido>(`${this.guestUrl}/${id}`, {headers: {'X-Guest-Token': token}});
    }
    return this.auth.isAuthenticated() ? this.http.get<Pedido>(`${this.apiUrl}/${id}`) : of(null);
  }
}
