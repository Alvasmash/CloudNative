import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient, HttpContext, HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FORM_HANDLES_ERRORS } from '../../core/interceptors/form-errors.context';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-password-recovery', standalone: true, imports: [FormsModule, RouterLink],
  templateUrl: './password-recovery.component.html', styleUrl: './password-recovery.component.css',
})
export class PasswordRecoveryComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  readonly reset = this.route.snapshot.data['reset'] === true;
  private readonly token = this.route.snapshot.queryParamMap.get('token') || '';
  readonly validToken = /^[A-Za-z0-9_-]{43}$/.test(this.token);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly done = signal(false);
  email = '';
  password = '';
  confirmation = '';

  submit(form: NgForm): void {
    if (form.invalid || this.busy() || (this.reset && !this.validToken)) return;
    this.error.set(''); this.message.set('');
    if (this.reset && this.password !== this.confirmation) {
      this.error.set('Las contraseñas no coinciden.'); return;
    }
    this.busy.set(true);
    const path = this.reset ? 'reset-password' : 'forgot-password';
    const body = this.reset ? {token: this.token, password: this.password} : {email: this.email.trim()};
    this.http.post<{message: string}>(environment.apiUrl + '/public/auth/' + path, body,
      {context: new HttpContext().set(FORM_HANDLES_ERRORS, true)})
      .pipe(finalize(() => this.busy.set(false))).subscribe({
        next: result => {
          this.message.set(result.message);
          if (this.reset) {
            this.password = ''; this.confirmation = ''; this.done.set(true);
            this.auth.clearEmailSession();
            history.replaceState(null, '', location.pathname + (environment.useHashRouting ? '#/restablecer-clave' : ''));
          }
        },
        error: (error: HttpErrorResponse) => this.error.set(error.error?.message ||
          (error.status === 0 ? 'No se pudo conectar. Comprueba tu conexión y vuelve a intentarlo.' : 'No se pudo completar la solicitud. Inténtalo de nuevo.')),
      });
  }
}
