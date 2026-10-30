import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { FormsModule, NgForm } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
@Component({
 selector: 'app-login', standalone: true, imports: [RouterModule, FormsModule],
 template: `
 <section class="welcome">
  <div class="story"><span class="eyebrow">BIENVENIDO A PEDIDOS360</span>
   <h1>Todo lo que te gusta.<br><em>A un pedido de distancia.</em></h1>
   <p>Tu próxima comida favorita empieza aquí. Explora el menú, elige a tu gusto y sigue cada paso de tu pedido.</p>
   <div class="food-art"><img src="images/product-1.jpg" alt="Pizza artesanal recién preparada" /><span class="photo-caption">Buenos ingredientes. Grandes momentos.</span></div>
   <div class="steps">01 &nbsp; Elige tu favorito &nbsp; · &nbsp; 02 &nbsp; Haz tu pedido &nbsp; · &nbsp; 03 &nbsp; Disfruta</div>
  </div>
  <div class="access-card"><span class="local-pill">● &nbsp; Entorno local</span>
   <h2>Bienvenido a<br>Pedidos<span>360</span></h2><p>Entra con tu cuenta o continúa como invitado.</p>
   <form #emailForm="ngForm" (ngSubmit)="loginWithEmail(emailForm)"><fieldset [disabled]="emailBusy()">
    <label>Correo electrónico<input name="email" type="email" [(ngModel)]="email" required email maxlength="254" autocomplete="username" placeholder="nombre@correo.com"></label>
    <label>Contraseña<input name="password" type="password" [(ngModel)]="password" required maxlength="72" autocomplete="current-password"></label>
    @if(emailError()){<p class="error" role="alert">{{emailError()}}</p>}
    <button class="email-submit" type="submit" [disabled]="emailForm.invalid || emailBusy()">{{emailBusy() ? 'Entrando…' : 'Iniciar sesión'}}</button>
   </fieldset></form>
   <p class="register-link">¿Primera vez aquí? <a routerLink="/registro">Crear una cuenta</a></p>
   <div class="separator">O usa tu cuenta Microsoft</div>
   <button class="role customer" (click)="loginWithMicrosoft()" [disabled]="authService.isLoading()"><span aria-hidden="true">⊞</span><span><strong>Continuar con Microsoft</strong><small>Cuenta institucional o invitada</small></span><b>↗</b></button>
   <a class="role" routerLink="/menu"><span aria-hidden="true">↗</span><span><strong>Continuar como invitado</strong><small>Explora y compra sin iniciar sesión</small></span></a>
   @if (authService.isLoading()) { <p role="status">Conectando con Microsoft…</p> }
   @if (authService.authError()) { <p class="error" role="alert">{{authService.authError()}}</p> }
   <div class="note">Iniciar sesión es opcional. Los administradores acceden con Microsoft.</div>
  </div>
 </section>`,
 styles: [`
 fieldset{border:0;padding:0;min-width:0}label{display:flex;flex-direction:column;gap:7px;font-size:12px;font-weight:650;color:#40583e;margin:16px 0}input{width:100%;border:1px solid #ccd8c3;border-radius:9px;padding:12px;font-size:14px;color:#213d2d;background:white}input:focus{outline:2px solid #779851;outline-offset:2px}.email-submit{width:100%;background:#183b30;color:white;border:0;border-radius:10px;padding:14px;font-size:13px;font-weight:650;cursor:pointer}.email-submit:disabled{opacity:.5}.register-link{font-size:12px!important;margin:16px 0!important}.register-link a{text-decoration:underline;color:#315839;font-weight:750}.separator{display:flex;align-items:center;gap:10px;font-size:11px;color:#6a7a62;margin:22px 0 14px}.separator:before,.separator:after{content:'';flex:1;height:1px;background:#e3e9dd}.error{font-size:12px;margin:10px 0}
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
 readonly emailBusy=signal(false);
 readonly emailError=signal('');
 email='';password='';
 constructor() { if(this.authService.isAuthenticated()) void this.router.navigateByUrl(this.authService.redirectDestination()); }
 async loginWithMicrosoft(): Promise<void> { await this.authService.loginWithMicrosoft(this.route.snapshot.queryParams['returnUrl']); }
 async loginWithEmail(form:NgForm):Promise<void>{
   if(form.invalid || this.emailBusy())return;
   this.emailBusy.set(true);this.emailError.set('');
   try{await this.authService.loginWithEmail(this.email.trim(),this.password);this.password='';
     const target=this.route.snapshot.queryParams['returnUrl'];
     await this.router.navigateByUrl(typeof target==='string'&&target.startsWith('/')&&!target.startsWith('//')?target:'/menu');}
   catch(error){const response=error as HttpErrorResponse;this.emailError.set(response.error?.message || 'No se pudo iniciar sesión. Comprueba tu correo y contraseña.');}
   finally{this.emailBusy.set(false);}
 }
}
