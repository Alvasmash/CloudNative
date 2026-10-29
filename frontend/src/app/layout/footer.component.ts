import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

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
            Solo localhost &bull; Datos guardados en este equipo
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
        background: #edf1e6;
        border-top: 1px solid rgba(255, 255, 255, 0.08);
        color: #64735c;
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
        color: #234b36;
        margin-bottom: 8px;
      }
      .text-orange {
        color: #234b36;
      }
      .footer-desc {
        font-size: 0.85rem;
        line-height: 1.5;
        margin: 0;
        color: #748073;
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
        background: rgba(255, 255, 255, 0.6);
        border: 1px solid #dce4d4;
        border-radius: 9999px;
        font-size: 0.775rem;
        font-weight: 600;
        color: #748073;
      }
      .status-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #10b981;
        box-shadow: 0 0 6px #10b981;
      }
      .copyright {
        font-size: 0.8rem;
        color: #748073;
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
  readonly currentYear = new Date().getFullYear();
}
