import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, of, switchMap, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';
import { AuthService } from '../auth/auth.service';
import { environment } from '../../../environments/environment';
import { OPTIONAL_SERVER_FEATURE } from './optional-feature.context';
import { FORM_HANDLES_ERRORS } from './form-errors.context';
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);
  const auth = inject(AuthService);
  const protectedRequest = req.url.startsWith(environment.apiUrl + '/api/');
  const request$ = protectedRequest ? from(auth.getAccessToken()).pipe(
    switchMap(token => next(req.clone({setHeaders: {Authorization: 'Bearer ' + token}})))) : next(req);
  return request$.pipe(catchError((error: HttpErrorResponse) => {
    if (req.context.get(FORM_HANDLES_ERRORS)) return throwError(() => error);
    if (error.status === 404 && req.context.get(OPTIONAL_SERVER_FEATURE)) return throwError(() => error);
    const message = error.status === 0 ? 'No se pudo conectar con el backend local.'
      : error.status === 401 || !error.status ? 'Inicia sesión para continuar.'
      : error.status === 403 ? 'Tu cuenta no tiene permiso para esta operación.'
      : error.error?.message || 'No se pudo completar la operación. Inténtalo de nuevo.';
    toast.error(message);
    return throwError(() => error);
  }));
};
