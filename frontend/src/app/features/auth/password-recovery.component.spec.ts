import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { PasswordRecoveryComponent } from './password-recovery.component';
import { AuthService } from '../../core/auth/auth.service';
import { environment } from '../../../environments/environment';

describe('Recuperación de contraseña', () => {
  const token = 'a'.repeat(43);
  let http: HttpTestingController;
  let clearSession: jasmine.Spy;
  async function setup(reset: boolean, linkToken = token) {
    clearSession = jasmine.createSpy('clearEmailSession');
    await TestBed.configureTestingModule({imports: [PasswordRecoveryComponent], providers: [
      provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      {provide: AuthService, useValue: {clearEmailSession: clearSession}},
      {provide: ActivatedRoute, useValue: {snapshot: {data: {reset}, queryParamMap: convertToParamMap({token: linkToken})}}},
    ]}).compileComponents();
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(PasswordRecoveryComponent);
    fixture.detectChanges(); await fixture.whenStable(); return fixture;
  }
  afterEach(() => http.verify());
  async function fill(fixture: any, name: string, value: string) {
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[name="' + name + '"]');
    input.value = value; input.dispatchEvent(new Event('input')); await fixture.whenStable(); fixture.detectChanges();
  }

  it('no envía contraseñas que no coinciden', async () => {
    const fixture = await setup(true);
    await fill(fixture, 'password', 'UnaClaveNueva123!'); await fill(fixture, 'confirmation', 'UnaClaveDiferente456!');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', {bubbles: true, cancelable: true}));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('no coinciden');
    http.expectNone(environment.apiUrl + '/public/auth/reset-password');
  });

  it('confirma el cambio y borra la sesión del cliente solo tras la respuesta del servidor', async () => {
    const fixture = await setup(true);
    await fill(fixture, 'password', 'UnaClaveNueva123!'); await fill(fixture, 'confirmation', 'UnaClaveNueva123!');
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', {bubbles: true, cancelable: true}));
    const request = http.expectOne(environment.apiUrl + '/public/auth/reset-password');
    expect(request.request.method).toBe('POST'); expect(request.request.body.token).toBe(token);
    expect(clearSession).not.toHaveBeenCalled();
    request.flush({message: 'Contraseña actualizada. Inicia sesión de nuevo.'}); await fixture.whenStable(); fixture.detectChanges();
    expect(clearSession).toHaveBeenCalled(); expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Contraseña actualizada');
    expect(fixture.nativeElement.querySelector('input[type="password"]')).toBeNull();
  });

  it('un enlace mal formado ofrece solicitar otro sin mostrar el formulario de cambio', async () => {
    const fixture = await setup(true, 'invalido');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('no es válido');
  });
});
