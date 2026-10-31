import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { PublicClientApplication, BrowserCacheLocation } from '@azure/msal-browser';
import { environment } from '../../../environments/environment';
import { AppUser, UserProfile } from '../models/user-profile.model';
import { mapBackendProfile } from './backend-profile';
import { FORM_HANDLES_ERRORS } from '../interceptors/form-errors.context';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly microsoft = new PublicClientApplication({
    auth: {
      clientId: environment.microsoft.clientId,
      authority: `https://login.microsoftonline.com/${environment.microsoft.tenantId}`,
      redirectUri: new URL('redirect', document.baseURI).href,
      postLogoutRedirectUri: new URL(environment.useHashRouting ? 'redirect' : 'login', document.baseURI).href,

    },
    cache: { cacheLocation: BrowserCacheLocation.SessionStorage },
    system: { loggerOptions: { piiLoggingEnabled: false } },
  });
  private token = '';
  private expiresAt = 0;
  private emailSession = false;
  readonly currentUser = signal<AppUser | null>(null);
  readonly isLoading = signal(false);
  readonly isDemoMode = signal(false);
  readonly authError = signal('');
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  readonly isAdmin = computed(() => this.currentUser()?.isAdmin ?? false);
  readonly userName = computed(() => this.currentUser()?.name ?? 'Invitado');
  readonly userEmail = computed(() => this.currentUser()?.email ?? '');

  async initialize(): Promise<void> {
    sessionStorage.removeItem('pedidos360_local_session');
    this.authError.set(sessionStorage.getItem('pedidos360_auth_error') || '');
    sessionStorage.removeItem('pedidos360_auth_error');
    try {
      await this.microsoft.initialize();
      const stored = sessionStorage.getItem('pedidos360_customer_session');
      if (stored) {
        try {
          const session = JSON.parse(stored);
          this.token = session.token; this.expiresAt = Number(session.expiresAt); this.emailSession = true;
          const user = await firstValueFrom(this.http.get<AppUser>(`${environment.apiUrl}/api/session`, {context: new HttpContext().set(FORM_HANDLES_ERRORS, true)}));
          this.currentUser.set({...user, isAdmin: false, roles: ['USER']});
          return;
        } catch {
          sessionStorage.removeItem('pedidos360_customer_session');
          this.token = ''; this.emailSession = false; this.expiresAt = 0;
        }
      }
      const result = await this.microsoft.handleRedirectPromise({ navigateToLoginRequestUrl: false });
      if (result?.account) this.microsoft.setActiveAccount(result.account);
      const account = this.microsoft.getActiveAccount() || this.microsoft.getAllAccounts()[0];
      if (!account) return;
      this.microsoft.setActiveAccount(account);
      if (result?.accessToken) { this.token = result.accessToken; this.expiresAt = result.expiresOn?.getTime() || 0; }
      await this.getAccessToken();
      const profile = await firstValueFrom(this.http.get<UserProfile>(`${environment.apiUrl}/api/profile`));
      this.currentUser.set(mapBackendProfile(profile, account));
    } catch {
      this.token = '';
      this.currentUser.set(null);
      this.authError.set('No se pudo validar la sesión de Microsoft. Inicia sesión nuevamente; comprueba el registro de la aplicación si el error continúa.');
      if (environment.useHashRouting && /\/redirect(?:\.html|\/)?$/.test(window.location.pathname)) sessionStorage.setItem('pedidos360_auth_error', this.authError());
    } finally {
      if (environment.useHashRouting && /\/redirect(?:\.html|\/)?$/.test(window.location.pathname)) {
        window.location.replace(new URL('#' + this.redirectDestination(), document.baseURI).href);
      }
    }
  }

  async loginWithMicrosoft(returnUrl?: string): Promise<void> {
    if (this.isLoading()) return;
    this.isLoading.set(true);
    this.authError.set('');
    if (returnUrl?.startsWith('/') && !returnUrl.startsWith('//')) sessionStorage.setItem('pedidos360_return_url', returnUrl);
    else sessionStorage.removeItem('pedidos360_return_url');
    try {
      await this.microsoft.loginRedirect({ scopes: ['openid', 'profile', 'email', environment.microsoft.apiScope], prompt: 'select_account' });
    } catch {
      this.authError.set('No se pudo abrir Microsoft. Revisa tu conexión y vuelve a intentar.');
      this.isLoading.set(false);
    }
  }

  async getAccessToken(): Promise<string> {
    if (this.token && this.expiresAt > Date.now() + 60000) return this.token;
    if (this.emailSession) {
      this.currentUser.set(null);
      sessionStorage.removeItem('pedidos360_customer_session');
      throw new Error('Tu sesión ha caducado. Inicia sesión de nuevo.');
    }
    const account = this.microsoft.getActiveAccount();
    if (!account) throw new Error('Inicia sesión con Microsoft para continuar.');
    const result = await this.microsoft.acquireTokenSilent({ account, scopes: [environment.microsoft.apiScope] });
    this.token = result.accessToken;
    this.expiresAt = result.expiresOn?.getTime() || 0;
    return this.token;
  }

  redirectDestination(): string {
    const target = sessionStorage.getItem('pedidos360_return_url');
    sessionStorage.removeItem('pedidos360_return_url');
    if (!this.isAuthenticated()) return '/login';
    return target?.startsWith('/') && !target.startsWith('//') ? target : (this.isAdmin() ? '/admin' : '/menu');
  }

  async logout(): Promise<void> {
    if (this.emailSession) {
      try { await firstValueFrom(this.http.post(`${environment.apiUrl}/api/auth/logout`, {}, {context: new HttpContext().set(FORM_HANDLES_ERRORS, true)})); }
      finally {
        sessionStorage.removeItem('pedidos360_customer_session');
        this.currentUser.set(null); this.token = ''; this.expiresAt = 0; this.emailSession = false;
        await this.router.navigate(['/menu']);
      }
      return;
    }
    this.currentUser.set(null);
    this.token = '';
    this.expiresAt = 0;
    try { await this.microsoft.logoutRedirect(); }
    catch { await this.microsoft.clearCache(); await this.router.navigate(['/login']); }
  }

  async loginWithEmail(email: string, password: string): Promise<void> {
    const result = await firstValueFrom(this.http.post<AppUser & {token: string; expiresAt: string}>(
      `${environment.apiUrl}/public/auth/login`, {email, password}, {context: new HttpContext().set(FORM_HANDLES_ERRORS, true)}));
    await this.microsoft.clearCache();
    this.emailSession = true; this.token = result.token; this.expiresAt = Date.parse(result.expiresAt);
    sessionStorage.setItem('pedidos360_customer_session', JSON.stringify({token: this.token, expiresAt: this.expiresAt}));
    this.currentUser.set({name: result.name, email: result.email, username: result.username, isAdmin: false, roles: ['USER']});
  }

  clearEmailSession(): void {
    if (!this.emailSession) return;
    sessionStorage.removeItem('pedidos360_customer_session');
    this.currentUser.set(null); this.token = ''; this.expiresAt = 0; this.emailSession = false;
  }
}
