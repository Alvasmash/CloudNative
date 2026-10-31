import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <footer class="footer">
      <div class="footer-container">
        <div class="footer-brand">
          <div class="footer-logo">
            <span class="logo-icon" aria-hidden="true">◉</span>
            <strong>Pedidos<span class="text-orange">360</span></strong>
          </div>
          <p class="footer-desc">
            Buenos sabores, pedidos simples. Tu restaurante, en un solo lugar.
          </p>
        </div>

        <div class="footer-meta">
          <div class="architecture-pill">
            <span class="status-dot"></span>
            {{ publicSite ? 'Buenos sabores, donde estés' : 'Solo localhost · Datos guardados en este equipo' }}
          </div>
          <p class="copyright">
            &copy; {{ currentYear }} Pedidos360 &bull; Duoc UC &bull; Hecho para disfrutar
          </p>
        </div>
      </div>
    </footer>
  `,
  styles: [
    `
      .footer {
        background:#17131b;
        border-top:1px solid rgba(255, 255, 255, 0.08);
        color:#d6a2bf;
        padding: 28px 0;
        margin-top: auto;
      }
      .footer-container {
        max-width: 1280px;
        margin: 0 auto;
        padding: 0 24px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 20px;
      }
      .footer-brand {
        max-width: 480px;
      }
      .footer-logo {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 1.15rem;
        font-weight: 800;
        color:var(--primary);
        margin-bottom: 8px;
      }
      .text-orange {
        color:var(--primary);
      }
      .footer-desc {
        font-size: 0.85rem;
        line-height: 1.5;
        margin: 0;
        color:#d6a2bf;
      }
      .footer-meta {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        gap: 8px;
      }
      .architecture-pill {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: 6px 14px;
        background:var(--bg-card);
        border:1px solid #4a2a40;
        border-radius:9999px;
        font-size: 0.775rem;
        font-weight: 600;
        color:#d6a2bf;
      }
      .status-dot {
        width: 8px;
        height: 8px;
        border-radius:50%;
        background:#12251d;
        box-shadow: 0 0 6px #10b981;
      }
      .copyright {
        font-size: 0.8rem;
        color:#d6a2bf;
        margin: 0;
      }
      @media (max-width: 768px) {
        .footer-container {
          flex-direction: column;
          align-items: flex-start;
        }
        .footer-meta {
          align-items: flex-start;
        }
      }
    `,
  ],
})
export class FooterComponent {
  readonly publicSite = environment.useHashRouting;
  readonly currentYear = new Date().getFullYear();
}
