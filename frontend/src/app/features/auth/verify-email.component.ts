import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient, HttpContext, HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { FORM_HANDLES_ERRORS } from '../../core/interceptors/form-errors.context';
import { finalize } from 'rxjs';
@Component({selector:'app-verify-email',standalone:true,imports:[RouterLink],
 templateUrl:'./verify-email.component.html',styleUrl:'./verify-email.component.css'})
export class VerifyEmailComponent {
  private readonly http=inject(HttpClient);
  private readonly token=inject(ActivatedRoute).snapshot.queryParamMap.get('token') || '';
  readonly validToken=/^[A-Za-z0-9_-]{43}$/.test(this.token);
  readonly busy=signal(false);readonly done=signal(false);readonly error=signal('');
  verify(){
    if(!this.validToken||this.busy()||this.done())return;
    this.busy.set(true);this.error.set('');
    this.http.post(environment.apiUrl+'/public/auth/verify',{token:this.token},{context:new HttpContext().set(FORM_HANDLES_ERRORS,true)})
      .pipe(finalize(()=>this.busy.set(false))).subscribe({next:()=>{this.done.set(true);history.replaceState(null,'',location.pathname);},
        error:(e:HttpErrorResponse)=>this.error.set(e.error?.message || 'No se pudo verificar el correo. Vuelve a intentarlo.')});
  }
}
