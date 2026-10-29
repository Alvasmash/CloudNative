import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
@Component({
 selector: 'app-login', standalone: true, imports: [RouterModule],
 template: `
 <section class="welcome">
  <div class="story"><span class="eyebrow">BIENVENIDO A PEDIDOS360</span>
   <h1>Todo lo que te gusta.<br><em>A un pedido de distancia.</em></h1>
   <p>Tu próxima comida favorita empieza aquí. Explora el menú, elige a tu gusto y sigue cada paso de tu pedido.</p>
   <div class="food-art"><img src="images/product-1.jpg" alt="Pizza artesanal recién preparada" /><span class="photo-caption">Buenos ingredientes. Grandes momentos.</span></div>
   <div class="steps">01 &nbsp; Elige tu favorito &nbsp; · &nbsp; 02 &nbsp; Haz tu pedido &nbsp; · &nbsp; 03 &nbsp; Disfruta</div>
  </div>
  <div class="access-card"><span class="local-pill">● &nbsp; Entorno local</span>
   <h2>Bienvenido a<br>Pedidos<span>360</span></h2><p>Puedes continuar sin cuenta o entrar con Microsoft.</p>
   <button class="role customer" (click)="loginWithMicrosoft()" [disabled]="authService.isLoading()"><span aria-hidden="true">⊞</span><span><strong>Continuar con Microsoft</strong><small>Cuenta institucional o invitada</small></span><b>↗</b></button>
   <a class="role" routerLink="/menu"><span aria-hidden="true">↗</span><span><strong>Continuar como invitado</strong><small>Explora y compra sin iniciar sesión</small></span></a>
   @if (authService.isLoading()) { <p role="status">Conectando con Microsoft…</p> }
   @if (authService.authError()) { <p class="error" role="alert">{{authService.authError()}}</p> }
   <div class="note">Iniciar sesión es opcional. Si entras con Microsoft, tu token determina el perfil de tu cuenta.</div>
  </div>
 </section>`,
 styles: [`
 .welcome{max-width:1240px;margin:auto;min-height:calc(100vh - 240px);display:grid;grid-template-columns:1.25fr 1fr;gap:72px;align-items:center;padding:64px 28px}.eyebrow{font-size:12px;font-weight:800;letter-spacing:.18em;color:#687b57}h1{font-size:clamp(34px,3.7vw,51px);line-height:1.08;letter-spacing:-.05em;margin:20px 0;color:#24392e}h1 em{color:#315839;font-style:normal}.story>p{max-width:440px;font-size:17px;color:#657268;line-height:1.8}
 .food-art{display:block;position:relative;max-width:460px;height:235px;overflow:hidden;border-radius:18px;margin:28px 0}.food-art img{width:100%;height:100%;object-fit:cover}.food-art .photo-caption{position:absolute;bottom:14px;left:14px;background:#ffffffed;color:#234b36;padding:8px 12px;border-radius:8px;font-size:11px;transform:none;width:auto;height:auto}.food-art span{display:grid;place-items:center;font-size:12px}.steps{color:#657268;font-size:12px;line-height:2}
 .access-card{padding:40px;background:white;border:1px solid #e0e7d9;border-radius:20px;box-shadow:0 16px 40px #293c2d06}.local-pill{display:inline-block;font-size:11px;background:#ecf3e8;color:#42613c;padding:6px 11px;border-radius:24px;font-weight:700}h2{font-size:32px;letter-spacing:-.04em;margin:24px 0 8px;color:#24392e}h2 span{color:#315839}.access-card>p{color:#657268;font-size:14px;margin-bottom:26px}
 .role{display:flex;align-items:center;gap:12px;width:100%;border:1px solid #e1e5dd;padding:20px 16px;border-radius:14px;background:#fff;text-align:left;cursor:pointer;color:#24392e;margin:12px 0;transition:transform .15s,border-color .15s}.role:hover{transform:translateY(-2px);border-color:#406a49}.role:disabled{opacity:.6;cursor:wait}.customer{background:#183b30;color:white;border-color:#243e30}.role>span:first-child{font-size:25px}.role strong,.role small{display:block}.role strong{font-size:14px}.role small{font-size:11px;opacity:.75;margin-top:5px}.role b{margin-left:auto;font-size:22px}.note{border-top:1px solid #eeeee8;padding-top:20px;margin-top:26px;font-size:12px;color:#65705e;line-height:1.8}.access-card .error{color:#b91c1c}
 @media(max-width:850px){.welcome{grid-template-columns:1fr;gap:30px;padding:36px 20px}.story{text-align:center}.story>p{margin:auto}.food-art{margin:25px auto}.welcome{min-width:0}.role{min-width:0}.role strong{overflow-wrap:anywhere}.food-art{justify-content:center}.access-card{padding:28px}}
 `]
})
export class LoginComponent {
 readonly authService = inject(AuthService);
 private readonly router = inject(Router);
 private readonly route = inject(ActivatedRoute);
 constructor() { if(this.authService.isAuthenticated()) void this.router.navigateByUrl(this.authService.redirectDestination()); }
 async loginWithMicrosoft(): Promise<void> { await this.authService.loginWithMicrosoft(this.route.snapshot.queryParams['returnUrl']); }
}
