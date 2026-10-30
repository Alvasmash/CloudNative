import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Producto } from '../models/producto.model';

export type ProductDraft = Omit<Producto, 'id'>;
@Injectable({providedIn: 'root'})
export class AdminCatalogService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl + '/api/admin/productos';
  list() { return this.http.get<Producto[]>(this.url); }
  create(product: ProductDraft) { return this.http.post<Producto>(this.url, product); }
  update(id: number, product: ProductDraft) { return this.http.put<Producto>(this.url + '/' + id, product); }
  remove(id: number) { return this.http.delete<void>(this.url + '/' + id); }
}

