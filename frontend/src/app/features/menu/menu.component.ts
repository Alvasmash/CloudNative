import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';
import { Producto } from '../../core/models/producto.model';
import { ClpCurrencyPipe } from '../../shared/pipes/clp-currency.pipe';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule, FormsModule, ClpCurrencyPipe],
  template: `
    <div class="menu-page">
      <!-- Hero Banner -->
      <section class="menu-hero">
        <div class="hero-content">
          <span class="hero-badge"
            >ELIGE. PIDE. DISFRUTA.</span
          >
          <h1 class="hero-title">Tu próximo favorito<br><span>está aquí.</span></h1>
          <p class="hero-subtitle">
            Pizzas recién hechas, hamburguesas irresistibles y algo fresco para acompañar. ¿Qué se te antoja hoy?
          </p>
          <div class="hero-note"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg> Preparado al momento · A tu gusto</div>
        </div>
        <img class="hero-photo" src="images/product-3.jpg" alt="Hamburguesa con queso, tomate y lechuga" fetchpriority="high" />
      </section>
      <div class="section-heading"><div><h2>Explora nuestro menú</h2><p>Elige tus favoritos. Nosotros nos encargamos del resto.</p></div><span class="item-count">{{ products().length }} opciones para ti</span></div>

      <!-- Filter, Search & Sort Bar -->
      <section class="controls-bar">
        <!-- Categories Tabs -->
        <div class="category-tabs">
          @for (cat of categories(); track cat) {
            <button
              type="button"
              class="tab-btn"
              [class.active]="selectedCategory() === cat" [attr.aria-pressed]="selectedCategory() === cat"
              (click)="selectCategory(cat)"
            >
              {{ cat }}
            </button>
          }
        </div>

        <!-- Search and Sort -->
        <div class="search-sort-group">
          <!-- Search input -->
          <div class="search-box">
            <svg
              viewBox="0 0 24 24"
              width="18"
              height="18"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              class="search-icon"
            >
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              type="text"
              aria-label="Buscar platos o ingredientes" placeholder="¿Qué se te antoja?"
              [ngModel]="searchQuery()"
              (ngModelChange)="searchQuery.set($event)"
              class="search-input"
            />
            @if (searchQuery()) {
              <button aria-label="Borrar búsqueda" class="clear-search" (click)="searchQuery.set('')">
                &times;
              </button>
            }
          </div>

          <!-- Sort Select -->
          <select
            [ngModel]="sortBy()"
            (ngModelChange)="sortBy.set($event)"
            aria-label="Ordenar productos" class="sort-select"
          >
            <option value="featured">Destacados</option>
            <option value="price-asc">Precio: Menor a Mayor</option>
            <option value="price-desc">Precio: Mayor a Menor</option>
            <option value="name">Nombre: A &rarr; Z</option>
          </select>
        </div>
      </section>

      <!-- Products Grid -->
      <section class="products-section">
        @if (loadError()) {
          <div class="empty-state" role="alert">
            <h2>No pudimos cargar el menú</h2>
            <p>Comprueba que el servidor local esté iniciado y vuelve a intentar.</p>
            <button type="button" (click)="ngOnInit()">Reintentar</button>
          </div>
        } @else if (isLoading()) {
          <!-- Skeleton Loading -->
          <div class="products-grid" aria-label="Cargando menú" aria-busy="true">
            @for (i of [1, 2, 3, 4, 5, 6]; track i) {
              <div class="skeleton-card">
                <div class="skeleton-img"></div>
                <div class="skeleton-body">
                  <div class="skeleton-line title"></div>
                  <div class="skeleton-line desc"></div>
                  <div class="skeleton-line desc short"></div>
                  <div class="skeleton-line footer"></div>
                </div>
              </div>
            }
          </div>
        } @else if (filteredProducts().length === 0) {
          <!-- Empty State -->
          <div class="empty-state">
            <div class="empty-icon">🔍</div>
            <h3>No se encontraron productos</h3>
            <p>
              Prueba con otros términos de búsqueda o selecciona otra categoría.
            </p>
            <button class="btn btn-outline" (click)="resetFilters()">
              Restablecer Filtros
            </button>
          </div>
        } @else {
          <div class="products-grid">
            @for (product of filteredProducts(); track product.id) {
              <article class="product-card">
                <!-- Image Wrapper with Badges -->
                <div class="card-img-wrapper">
                  <img
                    [src]="product.imagenUrl"
                    [alt]="product.nombre"
                    (error)="onImageError($event)"
                    loading="lazy"
                    class="card-img"
                  />
                  <span class="category-pill">{{ product.categoria }}</span>
                  @if (product.stock <= 0) {
                    <span class="stock-pill out-of-stock">Agotado</span>
                  } @else if (product.stock <= 10) {
                    <span class="stock-pill low-stock"
                      >¡Solo {{ product.stock }} disponibles!</span
                    >
                  }
                </div>

                <!-- Product Details -->
                <div class="card-body">
                  <h3 class="product-title">{{ product.nombre }}</h3>
                  <p class="product-desc">{{ product.descripcion }}</p>

                  <div class="card-footer">
                    <div class="price-container">
                      <span class="price-label">Precio</span>
                      <span class="price-value">{{
                        product.precio | clp
                      }}</span>
                    </div>

                    <button
                      type="button"
                      class="btn-add-cart"
                      [disabled]="product.stock <= 0" [attr.aria-label]="'Agregar ' + product.nombre"
                      (click)="addToCart(product)"
                    >
                      <svg
                        viewBox="0 0 24 24"
                        width="18"
                        height="18"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                      >
                        <path d="M12 5v14M5 12h14" />
                      </svg>
                      Agregar
                    </button>
                  </div>
                </div>
              </article>
            }
          </div>
        }
      </section>
    </div>
  `,
  styleUrl: './menu.component.css',
})
export class MenuComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);

  readonly products = signal<Producto[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly selectedCategory = signal<string>('Todos');
  readonly searchQuery = signal<string>('');
  readonly loadError = signal(false);
  readonly sortBy = signal<string>('featured');

  readonly categories = computed(() => {
    const list = this.products().map((p) => p.categoria);
    const unique = Array.from(new Set(list));
    return ['Todos', ...unique];
  });

  readonly filteredProducts = computed(() => {
    let list = this.products();

    // Filtro por categoría
    if (this.selectedCategory() !== 'Todos') {
      list = list.filter(
        (p) =>
          p.categoria.toLowerCase() === this.selectedCategory().toLowerCase(),
      );
    }

    // Filtro por búsqueda
    const q = this.searchQuery().trim().toLowerCase();
    if (q) {
      list = list.filter(
        (p) =>
          p.nombre.toLowerCase().includes(q) ||
          p.descripcion.toLowerCase().includes(q),
      );
    }

    // Ordenamiento
    const sort = this.sortBy();
    const sorted = [...list];
    if (sort === 'price-asc') {
      sorted.sort((a, b) => a.precio - b.precio);
    } else if (sort === 'price-desc') {
      sorted.sort((a, b) => b.precio - a.precio);
    } else if (sort === 'name') {
      sorted.sort((a, b) => a.nombre.localeCompare(b.nombre));
    }

    return sorted;
  });

  ngOnInit(): void {
    this.isLoading.set(true);
    this.loadError.set(false);
    this.productService.getProductos().subscribe({
      next: (items) => {
        this.products.set(items);
        this.isLoading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  selectCategory(category: string): void {
    this.selectedCategory.set(category);
  }

  resetFilters(): void {
    this.selectedCategory.set('Todos');
    this.searchQuery.set('');
    this.sortBy.set('featured');
  }

  addToCart(product: Producto): void {
    this.cartService.addItem(product, 1);
  }

  onImageError(event: Event): void {
    const image = event.target as HTMLImageElement;
    image.onerror = null;
    image.src = new URL('images/menu.svg', document.baseURI).href;
  }
}
