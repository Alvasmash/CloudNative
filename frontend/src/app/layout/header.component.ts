import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { CartService } from '../core/services/cart.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <header class="navbar">
      <div class="nav-container">
        <!-- Brand Logo -->
        <a routerLink="/menu" class="brand">
          <div class="brand-icon">
            <svg
              viewBox="0 0 24 24"
              width="22"
              height="22"
              fill="none"
              stroke="currentColor"
              stroke-width="2.5"
            >
              <path
                d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z"
              />
              <path d="M12 6v6l4 2" />
            </svg>
          </div>
          <span class="brand-name"
            >Pedidos<span class="brand-highlight">360</span></span
          >
        </a>

        <!-- Desktop Navigation Links -->
        <nav class="nav-links">
          <a routerLink="/menu" routerLinkActive="active" class="nav-link">
            <svg
              viewBox="0 0 24 24"
              width="18"
              height="18"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M3 3h18v18H3z" />
              <path d="M3 9h18M9 21V9" />
            </svg>
            Menú
          </a>
          <a routerLink="/pedidos" routerLinkActive="active" class="nav-link">
              <svg
                viewBox="0 0 24 24"
                width="18"
                height="18"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"
                />
                <polyline points="14 2 14 8 20 8" />
                <line x1="16" y1="13" x2="8" y2="13" />
                <line x1="16" y1="17" x2="8" y2="17" />
                <polyline points="10 9 9 9 8 9" />
              </svg>
              Mis Pedidos
            </a>
          @if (authService.isAdmin()) {
            <a
              routerLink="/admin"
              routerLinkActive="active"
              class="nav-link nav-link-admin"
            >
              <svg
                viewBox="0 0 24 24"
                width="18"
                height="18"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z" />
                <path
                  d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"
                />
              </svg>
              Administración
              <span class="admin-chip">ADMIN</span>
            </a>
          }
        </nav>

        <!-- Right Side: Cart + User Profile / Login -->
        <div class="nav-actions">
          <!-- Cart Icon with badge -->
          <a routerLink="/carrito" class="cart-btn" title="Ver Carrito">
            <svg
              viewBox="0 0 24 24"
              width="22"
              height="22"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle cx="9" cy="21" r="1" />
              <circle cx="20" cy="21" r="1" />
              <path
                d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"
              />
            </svg>
            @if (cartService.totalCount() > 0) {
              <span class="cart-badge">{{ cartService.totalCount() }}</span>
            }
          </a>

          <!-- User Profile / Auth State -->
          @if (authService.isAuthenticated()) {
            <div class="user-menu">
              <button type="button" class="user-pill" (click)="toggleDropdown()" [attr.aria-expanded]="isDropdownOpen()" aria-label="Abrir menú de usuario">
                <div class="user-avatar">{{ userInitials }}</div>
                <div class="user-info">
                  <span class="user-name">{{ authService.userName() }}</span>
                  <span class="user-role">{{
                    authService.isAdmin() ? 'Administrador' : 'Cliente'
                  }}</span>
                </div>
                <svg
                  viewBox="0 0 24 24"
                  width="16"
                  height="16"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M6 9l6 6 6-6" />
                </svg>
              </button>

              @if (isDropdownOpen()) {
                <div class="dropdown-menu">
                  <div class="dropdown-header">
                    <strong>{{ authService.userName() }}</strong>
                    <small>{{ authService.userEmail() }}</small>
                  </div>
                  <div class="dropdown-divider"></div>
                  <a
                    routerLink="/pedidos"
                    class="dropdown-item"
                    (click)="closeDropdown()"
                  >
                    Mis Pedidos
                  </a>
                  @if (authService.isAdmin()) {
                    <a
                      routerLink="/admin"
                      class="dropdown-item"
                      (click)="closeDropdown()"
                    >
                      Panel de Administración
                    </a>
                  }
                  <div class="dropdown-divider"></div>
                  <button
                    type="button"
                    class="dropdown-item text-danger"
                    (click)="logout()"
                  >
                    Cerrar Sesión
                  </button>
                </div>
              }
            </div>
          } @else {
            <a routerLink="/login" class="btn btn-login"> Iniciar Sesión </a>
          }

          <!-- Mobile Hamburger Toggle -->
          <button
            type="button"
            class="mobile-toggle"
            (click)="toggleMobileNav()"
            aria-label="Menú móvil" [attr.aria-expanded]="isMobileNavOpen()"
          >
            <svg
              viewBox="0 0 24 24"
              width="24"
              height="24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Mobile Navigation Drawer -->
      @if (isMobileNavOpen()) {
        <div class="mobile-nav">
          <a
            routerLink="/menu"
            (click)="closeMobileNav()"
            class="mobile-nav-link"
            >Menú</a
          >
          @if (authService.isAuthenticated()) {
            <a
              routerLink="/pedidos"
              (click)="closeMobileNav()"
              class="mobile-nav-link"
              >Mis Pedidos</a
            >
          }
          @if (authService.isAdmin()) {
            <a
              routerLink="/admin"
              (click)="closeMobileNav()"
              class="mobile-nav-link text-warning"
              >Administración</a
            >
          }
          <a
            routerLink="/carrito"
            (click)="closeMobileNav()"
            class="mobile-nav-link"
          >
            Carrito ({{ cartService.totalCount() }})
          </a>
          @if (authService.isAuthenticated()) {
            <button
              type="button"
              class="mobile-nav-link btn-logout"
              (click)="logout()"
            >
              Cerrar Sesión
            </button>
          } @else {
            <a
              routerLink="/login"
              (click)="closeMobileNav()"
              class="mobile-nav-link text-primary"
              >Iniciar Sesión</a
            >
          }
        </div>
      }
    </header>
  `,
  styleUrl: './header.component.css',
})
export class HeaderComponent {
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);

  readonly isDropdownOpen = signal<boolean>(false);
  readonly isMobileNavOpen = signal<boolean>(false);

  get userInitials(): string {
    const name = this.authService.userName();
    return (
      name
        .split(' ')
        .slice(0, 2)
        .map((part) => part[0])
        .join('')
        .toUpperCase() || 'U'
    );
  }

  toggleDropdown(): void {
    this.isDropdownOpen.update((v) => !v);
  }

  closeDropdown(): void {
    this.isDropdownOpen.set(false);
  }

  toggleMobileNav(): void {
    this.isMobileNavOpen.update((v) => !v);
  }

  closeMobileNav(): void {
    this.isMobileNavOpen.set(false);
  }


  logout(): void {
    this.closeDropdown();
    this.closeMobileNav();
    this.authService.logout();
  }
}
