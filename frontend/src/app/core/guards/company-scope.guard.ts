import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class CompanyScopeGuard implements CanActivate {
  constructor(private router: Router) {}

  canActivate(): boolean {
    const tenantId = (localStorage.getItem('tenantId') || '').trim();
    const companyId = (localStorage.getItem('empresaContextId') || '').trim();
    if (tenantId && companyId) {
      return true;
    }
    this.router.navigateByUrl('/home');
    return false;
  }
}
