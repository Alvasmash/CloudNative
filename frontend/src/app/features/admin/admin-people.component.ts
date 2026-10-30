import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../environments/environment';
import { AdminNavComponent } from './admin-nav.component';
import { finalize } from 'rxjs';
interface Person {id:string;nombre:string;email:string;proveedor:string;verificado:boolean;rol:string;fechaRegistro:string;ultimoAcceso:string|null}
interface PeoplePage {personas:Person[];pagina:number;totalPaginas:number;totalPersonas:number}
@Component({selector:'app-admin-people',standalone:true,imports:[AdminNavComponent,DatePipe,FormsModule],
 templateUrl:'./admin-people.component.html',styleUrl:'./admin-people.component.css'})
export class AdminPeopleComponent implements OnInit {
  private readonly http = inject(HttpClient);
  readonly data = signal<PeoplePage>({personas:[],pagina:0,totalPaginas:0,totalPersonas:0});
  readonly loading = signal(false); readonly error = signal('');
  query = '';
  ngOnInit(){this.load();}
  load(page=0){
    this.loading.set(true);this.error.set('');
    const params = new HttpParams().set('page',page).set('q',this.query.trim());
    this.http.get<PeoplePage>(environment.apiUrl+'/api/admin/usuarios',{params}).pipe(finalize(()=>this.loading.set(false))).subscribe({
      next:data=>this.data.set(data),error:()=>this.error.set('No se pudo cargar el listado de personas. Vuelve a intentarlo.')});
  }
}
