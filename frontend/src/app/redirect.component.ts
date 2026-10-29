import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
@Component({selector:'app-redirect',standalone:true,template:'<p>Validando tu cuenta de Microsoft…</p>'})
export class RedirectComponent { constructor(){ const auth=inject(AuthService); void inject(Router).navigateByUrl(auth.redirectDestination()); } }
