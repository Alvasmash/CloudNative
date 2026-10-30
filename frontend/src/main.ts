import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { environment } from './environments/environment';

// Preserve old Pages links while making all new navigation refresh-safe with hashes.
if (environment.useHashRouting && !window.location.hash) {
  const base = new URL(document.baseURI);
  const oldPath = window.location.pathname.startsWith(base.pathname)
    ? window.location.pathname.slice(base.pathname.length).replace(/\/$/, '') : '';
  if (/^(menu|carrito|pedidos(?:\/\d+)?|admin(?:\/(?:productos|personas))?|login|registro|verificar-correo)$/.test(oldPath)) {
    window.history.replaceState(null, '', base.href + '#/' + oldPath + window.location.search);
  }
}

bootstrapApplication(AppComponent, appConfig).catch((err) =>
  console.error(err),
);
