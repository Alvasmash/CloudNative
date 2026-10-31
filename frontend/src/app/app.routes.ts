import { Routes } from '@angular/router';

import { adminGuard } from './core/guards/admin.guard';
import { RedirectComponent } from './redirect.component';
export const routes: Routes = [
  {path: 'recuperar-clave', loadComponent: () => import('./features/auth/password-recovery.component').then(m => m.PasswordRecoveryComponent), title: 'Recuperar contraseña | Pedidos360'},
  {path: 'restablecer-clave', loadComponent: () => import('./features/auth/password-recovery.component').then(m => m.PasswordRecoveryComponent), data: {reset: true}, title: 'Nueva contraseña | Pedidos360'},
  {path: 'registro', loadComponent: () => import('./features/auth/register.component').then(m => m.RegisterComponent), title: 'Crear cuenta | Pedidos360'},
  {path: 'verificar-correo', loadComponent: () => import('./features/auth/verify-email.component').then(m => m.VerifyEmailComponent), title: 'Verificar correo | Pedidos360'},
  {path: 'admin/productos', loadComponent: () => import('./features/admin/admin-products.component').then(m => m.AdminProductsComponent), canActivate: [adminGuard], title: 'Productos | Administración'},
  {path: 'admin/personas', loadComponent: () => import('./features/admin/admin-people.component').then(m => m.AdminPeopleComponent), canActivate: [adminGuard], title: 'Personas | Administración'},
  // Microsoft redirect callback; the application initializer validates the response.
  {
    path: 'redirect',
    component: RedirectComponent,
    title: 'Procesando autenticación',
  },

  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'menu',
  },

  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.component').then((m) => m.LoginComponent),
    title: 'Iniciar Sesión | Pedidos360',
  },

  {
    path: 'menu',
    loadComponent: () =>
      import('./features/menu/menu.component').then((m) => m.MenuComponent),
    title: 'Menú & Catálogo | Pedidos360',
  },

  {
    path: 'carrito',
    loadComponent: () =>
      import('./features/cart/cart.component').then((m) => m.CartComponent),
    title: 'Carrito de Compras | Pedidos360',
  },

  {
    path: 'pedidos',
    loadComponent: () =>
      import('./features/orders/orders-list.component').then(
        (m) => m.OrdersListComponent,
      ),
    title: 'Mis Pedidos | Pedidos360',
  },

  {
    path: 'pedidos/:id',
    loadComponent: () =>
      import('./features/orders/order-detail.component').then(
        (m) => m.OrderDetailComponent,
      ),
    title: 'Seguimiento de Pedido | Pedidos360',
  },

  {
    path: 'admin',
    loadComponent: () =>
      import('./features/admin/admin-dashboard.component').then(
        (m) => m.AdminDashboardComponent,
      ),
    canActivate: [adminGuard],
    title: 'Administración de Pedidos | Pedidos360',
  },

  {
    path: '**',
    redirectTo: 'menu',
  },
];
