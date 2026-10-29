import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '../auth/auth.service';
import { OrderService } from './order.service';

describe('Guest ordering', () => {
  let service: OrderService;
  let http: HttpTestingController;
  const url = 'http://localhost:8080/public/guest/pedidos';
  beforeEach(() => {
    sessionStorage.removeItem('pedidos360_guest_access');
    sessionStorage.removeItem('pedidos360_guest_order_ids');
    TestBed.configureTestingModule({providers: [OrderService, provideHttpClient(), provideHttpClientTesting(),
      {provide: AuthService, useValue: {isAuthenticated: () => false}}]});
    service = TestBed.inject(OrderService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    http.verify();
    sessionStorage.removeItem('pedidos360_guest_access');
    sessionStorage.removeItem('pedidos360_guest_order_ids');
  });
  it('creates with a private random guest key and no Microsoft bearer token', () => {
    service.crearPedido({clienteNombre: 'Invitado', clienteEmail: 'guest@example.com', items: [{productoId: 1, cantidad: 1}]}).subscribe();
    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('X-Guest-Token')).toMatch(/^[A-Za-z0-9_-]{43}$/);
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({id: 42, numeroPedido: 'PED-GUEST'});
    expect(JSON.parse(sessionStorage.getItem('pedidos360_guest_order_ids')!)).toEqual([42]);
  });
  it('returns an empty history without asking a new visitor to log in', () => {
    service.getMisPedidos().subscribe(orders => expect(orders).toEqual([]));
    http.expectNone(url);
  });
  it('tracks the actual server order using the same guest key', () => {
    sessionStorage.setItem('pedidos360_guest_access', 'A'.repeat(43));
    service.getPedidoById(42).subscribe(order => expect(order?.id).toBe(42));
    const req = http.expectOne(url + '/42');
    expect(req.request.headers.get('X-Guest-Token')).toBe('A'.repeat(43));
    req.flush({id: 42});
  });
});
