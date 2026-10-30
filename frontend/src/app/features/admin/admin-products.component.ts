import { Component, ElementRef, ViewChild, computed, inject, signal, OnInit } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { AdminCatalogService, ProductDraft } from '../../core/services/admin-catalog.service';
import { Producto } from '../../core/models/producto.model';
import { ToastService } from '../../core/services/toast.service';
import { ClpCurrencyPipe } from '../../shared/pipes/clp-currency.pipe';
import { AdminNavComponent } from './admin-nav.component';
import { finalize } from 'rxjs';
import { RouterLink } from '@angular/router';

@Component({selector: 'app-admin-products', standalone: true, imports: [FormsModule, ClpCurrencyPipe, AdminNavComponent, RouterLink],
  templateUrl: './admin-products.component.html', styleUrl: './admin-products.component.css'})
export class AdminProductsComponent implements OnInit {
  private readonly api = inject(AdminCatalogService);
  private readonly toast = inject(ToastService);
  @ViewChild('deleteDialog') deleteDialog!: ElementRef<HTMLDialogElement>;
  readonly products = signal<Producto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly saving = signal(false);
  readonly deleting = signal(false);
  readonly editorOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly search = signal('');
  readonly selected = signal<Producto | null>(null);
  readonly photos = [1,2,3,4,5,6];
  readonly filtered = computed(() => this.products().filter(p =>
    (p.nombre + ' ' + p.categoria).toLowerCase().includes(this.search().trim().toLowerCase())));
  readonly outOfStock = computed(() => this.products().filter(p => p.stock === 0).length);
  draft: ProductDraft = this.empty();
  ngOnInit() { this.load(); }
  empty(): ProductDraft { return {nombre:'', descripcion:'', precio:1000, categoria:'', stock:0, imagenUrl:'images/product-1.jpg'}; }
  load() {
    this.loading.set(true); this.error.set('');
    this.api.list().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: products => this.products.set(products), error: () => this.error.set('No se pudo cargar el catálogo. Vuelve a intentarlo.')});
  }
  edit(product?: Producto) {
    this.editingId.set(product?.id ?? null);
    this.draft = product ? {nombre:product.nombre, descripcion:product.descripcion, precio:product.precio,
      categoria:product.categoria, stock:product.stock, imagenUrl:this.photo(product)} : this.empty();
    this.error.set(''); this.editorOpen.set(true);
    setTimeout(() => document.getElementById('product-name')?.focus());
  }
  save(form: NgForm) {
    if (form.invalid || this.saving() || !this.draft.nombre.trim() || !this.draft.descripcion.trim() || !this.draft.categoria.trim()) return;
    this.saving.set(true); this.error.set('');
    const id = this.editingId();
    const request = id === null ? this.api.create(this.draft) : this.api.update(id, this.draft);
    request.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: saved => { this.products.update(list => [saved, ...list.filter(p => p.id !== saved.id)]);
        this.editorOpen.set(false); this.toast.success(id === null ? 'Producto agregado al menú.' : 'Producto actualizado.'); },
      error: () => this.error.set('No se pudo guardar. Revisa los datos y vuelve a intentarlo.')
    });
  }
  askDelete(product: Producto) { this.selected.set(product); this.deleteDialog.nativeElement.showModal(); }
  cancelDelete() { if (!this.deleting()) this.deleteDialog.nativeElement.close(); }
  remove() {
    const product = this.selected(); if (!product || this.deleting()) return;
    this.deleting.set(true);
    this.api.remove(product.id).pipe(finalize(() => this.deleting.set(false))).subscribe({
      next: () => { this.products.update(list => list.filter(p => p.id !== product.id)); this.deleteDialog.nativeElement.close();
        this.toast.success('Producto eliminado del menú.'); },
      error: () => { this.deleteDialog.nativeElement.close(); this.error.set('No se pudo eliminar el producto. Vuelve a intentarlo.'); }
    });
  }
  photo(product: Producto) { return product.imagenUrl || 'images/product-' + (product.id >= 1 && product.id <= 6 ? product.id : 1) + '.jpg'; }
}
