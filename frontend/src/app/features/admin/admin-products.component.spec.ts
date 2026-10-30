import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AdminProductsComponent } from './admin-products.component';
import { environment } from '../../../environments/environment';

describe('Gestión de productos', () => {
  const product = {id:91,nombre:'Pizza de prueba',descripcion:'Mozzarella y tomate',categoria:'Pizzas',precio:4500,stock:4,imagenUrl:'images/product-2.jpg'};
  const url = environment.apiUrl + '/api/admin/productos';
  let http: HttpTestingController;
  async function setup() {
    await TestBed.configureTestingModule({imports:[AdminProductsComponent],providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([])]}).compileComponents();
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(AdminProductsComponent);
    fixture.detectChanges(); http.expectOne(url).flush([product]); await fixture.whenStable();fixture.detectChanges();
    return fixture;
  }
  afterEach(()=>http.verify());

  it('edita un producto y muestra el precio confirmado por el servidor', async()=>{
    const fixture=await setup();
    fixture.nativeElement.querySelector('[aria-label="Editar Pizza de prueba"]').click();
    fixture.detectChanges();await fixture.whenStable();
    const input:HTMLInputElement=fixture.nativeElement.querySelector('input[name="precio"]');
    input.value='6900';input.dispatchEvent(new Event('input'));await fixture.whenStable();fixture.detectChanges();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit',{bubbles:true,cancelable:true}));
    const request=http.expectOne(url+'/91');expect(request.request.method).toBe('PUT');expect(request.request.body.precio).toBe(6900);
    request.flush({...product,precio:6900});await fixture.whenStable();fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.product-meta').textContent).toContain('6.900');
    expect(fixture.nativeElement.querySelector('.editor')).toBeNull();
  });

  it('cancelar la confirmación no elimina; confirmar espera la respuesta del servidor', async()=>{
    const fixture=await setup();
    fixture.nativeElement.querySelector('[aria-label="Eliminar Pizza de prueba"]').click();fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('dialog').open).toBeTrue();
    fixture.nativeElement.querySelector('dialog .secondary').click();fixture.detectChanges();
    http.expectNone(url+'/91');expect(fixture.componentInstance.products().length).toBe(1);
    fixture.nativeElement.querySelector('[aria-label="Eliminar Pizza de prueba"]').click();fixture.detectChanges();
    fixture.nativeElement.querySelector('dialog .danger').click();fixture.detectChanges();
    const request=http.expectOne(url+'/91');expect(request.request.method).toBe('DELETE');
    expect(fixture.componentInstance.products().length).toBe(1);
    request.flush(null,{status:204,statusText:'No Content'});await fixture.whenStable();fixture.detectChanges();
    expect(fixture.componentInstance.products().length).toBe(0);
  });
});
