import { TestBed, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { KafkaMonitorComponent } from './kafka-monitor.component';
import { errorInterceptor } from '../../core/interceptors/error.interceptor';
import { AuthService } from '../../core/auth/auth.service';
import { ToastService } from '../../core/services/toast.service';

describe('Kafka server compatibility', () => {
  let http: HttpTestingController;
  let toast: jasmine.SpyObj<ToastService>;
  const url = 'http://localhost:8080/api/admin/kafka';
  beforeEach(() => {
    toast = jasmine.createSpyObj('ToastService', ['error']);
    TestBed.configureTestingModule({imports: [KafkaMonitorComponent], providers: [
      provideHttpClient(withInterceptors([errorInterceptor])), provideHttpClientTesting(),
      {provide: AuthService, useValue: {getAccessToken: () => Promise.resolve('test-only-token')}},
      {provide: ToastService, useValue: toast},
    ]});
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('explains a missing Kafka endpoint without simulating a connection or events', fakeAsync(() => {
    const fixture = TestBed.createComponent(KafkaMonitorComponent);
    fixture.detectChanges(); flushMicrotasks();
    http.expectOne(url).flush({}, {status: 404, statusText: 'Not Found'});
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Este servidor no ofrece el panel de Kafka');
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
    expect(fixture.componentInstance.data()).toBeNull();
    expect(toast.error).not.toHaveBeenCalled();
  }));
  it('still displays permission failures instead of treating them as missing features', fakeAsync(() => {
    const fixture = TestBed.createComponent(KafkaMonitorComponent);
    fixture.detectChanges(); flushMicrotasks();
    http.expectOne(url).flush({}, {status: 403, statusText: 'Forbidden'});
    fixture.detectChanges();
    expect(fixture.componentInstance.unavailable()).toBeFalse();
    expect(toast.error).toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
  }));
  it('continues to show real events when the server supports Kafka', fakeAsync(() => {
    const fixture = TestBed.createComponent(KafkaMonitorComponent);
    fixture.detectChanges(); flushMicrotasks();
    http.expectOne(url).flush({status: 'CONNECTED', topic: 'pedidos.eventos', retentionDays: 7, events: [
      {event: {eventId: 'test-event', type: 'PEDIDO_CREADO', pedidoId: 1, numeroPedido: 'PED-REAL', estado: 'PENDIENTE', occurredAt: '2026-10-29T20:00:00Z'}, partition: 0, offset: 12},
    ]});
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Broker conectado');
    expect(fixture.nativeElement.textContent).toContain('PED-REAL');
    expect(toast.error).not.toHaveBeenCalled();
  }));
});
