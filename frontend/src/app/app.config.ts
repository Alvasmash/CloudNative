import { ApplicationConfig, provideAppInitializer, inject, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding, withHashLocation } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { AuthService } from './core/auth/auth.service';
import { environment } from '../environments/environment';
import { routes } from './app.routes';
import { errorInterceptor } from './core/interceptors/error.interceptor';
export const appConfig: ApplicationConfig = {
  providers: [provideAppInitializer(() => inject(AuthService).initialize()),provideZoneChangeDetection({ eventCoalescing: true }), provideRouter(routes, withComponentInputBinding(), ...(environment.useHashRouting ? [withHashLocation()] : [])), provideHttpClient(withInterceptors([errorInterceptor]))],
};
