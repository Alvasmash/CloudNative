import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Producto } from '../models/producto.model';
@Injectable({providedIn:'root'})
export class ProductService {
 private readonly http=inject(HttpClient);
 private readonly apiUrl=`${environment.apiUrl}/public/productos`;
 getProductos():Observable<Producto[]>{return this.http.get<Producto[]>(this.apiUrl).pipe(map(list=>list.map(p=>this.withImage(p))));}
 getProductoById(id:number):Observable<Producto|null>{return this.http.get<Producto>(`${this.apiUrl}/${id}`).pipe(map(p=>this.withImage(p)));}
 private withImage(p:Producto):Producto{return {...p,imagenUrl:p.imagenUrl || `images/product-${p.id>=1&&p.id<=6?p.id:1}.jpg`};}
}
