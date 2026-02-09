import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
    const loggedIn = await this.auth.isLoggedIn();
    if (!loggedIn) {
      await this.auth.login();
      return false;
    }
    const tenantId = localStorage.getItem('tenantId');
    const url = state?.url || '';
    if (!tenantId && url !== '/' && url !== '') {
      this.router.navigateByUrl('/');
      return false;
    }
    return true;
  }
}
