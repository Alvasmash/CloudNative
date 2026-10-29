import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AdminDashboardResponse } from '../models/admin-dashboard.model';
import { Pedido } from '../models/pedido.model';
import { EstadoPedido } from '../models/estado-pedido.model';
import { ToastService } from './toast.service';
@Injectable({providedIn:'root'})
export class AdminService {
 private readonly http=inject(HttpClient);
 private readonly toast=inject(ToastService);
 private readonly apiUrl=`${environment.apiUrl}/api/admin/pedidos`;
 getDashboardData():Observable<AdminDashboardResponse>{return this.http.get<AdminDashboardResponse>(this.apiUrl);}
 actualizarEstado(id:number, estado:EstadoPedido):Observable<Pedido>{
  return this.http.patch<Pedido>(`${this.apiUrl}/${id}/estado`,{estado}).pipe(tap(()=>this.toast.success(`Estado actualizado a ${estado}`)));
 }
 eliminarPedido(id:number):Observable<void>{
  return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(tap(()=>this.toast.success(`Pedido #${id} eliminado`)));
 }
}
