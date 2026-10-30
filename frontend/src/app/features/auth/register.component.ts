import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient, HttpContext, HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { FORM_HANDLES_ERRORS } from '../../core/interceptors/form-errors.context';
import { finalize } from 'rxjs';
@Component({selector:'app-register',standalone:true,imports:[FormsModule,RouterLink],
 templateUrl:'./register.component.html',styleUrl:'./register.component.css'})
export class RegisterComponent implements OnInit {
  private readonly http=inject(HttpClient);
  readonly busy=signal(false); readonly error=signal(''); readonly message=signal(''); readonly localMail=signal(false);
  nombre=''; email=''; password=''; confirmation='';
  ngOnInit(){this.http.get<{mode:string}>(environment.apiUrl+'/public/auth/mail-mode').subscribe({
    next:data=>this.localMail.set(data.mode==='LOCAL'),error:()=>{} });}
  register(form:NgForm){
    if(form.invalid || this.busy())return;
    if(this.password!==this.confirmation){this.error.set('Las contraseñas no coinciden.');return;}
    this.send('register',{nombre:this.nombre.trim(),email:this.email.trim(),password:this.password});
  }
  resend(){if(!this.email || this.busy())return;this.send('resend',{email:this.email.trim()});}
  private send(action:string,body:object){
    this.busy.set(true);this.error.set('');this.message.set('');
    this.http.post<{message:string}>(environment.apiUrl+'/public/auth/'+action,body,{context:new HttpContext().set(FORM_HANDLES_ERRORS,true)})
      .pipe(finalize(()=>this.busy.set(false))).subscribe({
        next:data=>{this.message.set(data.message);this.password='';this.confirmation='';},
        error:(error:HttpErrorResponse)=>this.error.set(error.error?.message || 'No se pudo completar el registro. Comprueba la conexión y los datos.')
      });
  }
}
