import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { environment } from '../../../environments/environment';
import { TenantService, LocatarioResponse } from '../tenants/tenant.service';
import { CompanyService, EmpresaResponse } from '../companies/company.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatFormFieldModule, MatInputModule, FormsModule, MatSelectModule, MatIconModule],
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {
  me: any = null;
  tenantId: number | null = localStorage.getItem('tenantId') ? Number(localStorage.getItem('tenantId')) : null;
  allowedTenants: LocatarioResponse[] = [];

  constructor(
    private tenantService: TenantService,
    private companyService: CompanyService,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadMe();
    this.loadAllowedTenants();
  }

  saveTenant() {
    if (this.tenantId === null || this.tenantId === undefined) return;
    const tenantId = String(this.tenantId);
    localStorage.setItem('tenantId', tenantId);
    localStorage.removeItem('empresaContextId');
    localStorage.removeItem('empresaContextTipo');
    localStorage.removeItem('empresaContextNome');

    this.companyService.list({ page: 0, size: 500 }).subscribe({
      next: data => {
        const items = (data?.content || []) as EmpresaResponse[];
        const selected = items.find(e => !!e.padrao);
        this.applyEmpresaContext(selected);
        this.finishTenantSelection();
      },
      error: () => {
        this.applyEmpresaContext(undefined);
        this.finishTenantSelection();
      }
    });
  }

  private applyEmpresaContext(selected?: EmpresaResponse): void {
    if (selected) {
      localStorage.setItem('empresaContextId', String(selected.id));
      localStorage.setItem('empresaContextTipo', selected.tipo || '');
      localStorage.setItem('empresaContextNome', selected.razaoSocial || '');
      return;
    }
    localStorage.removeItem('empresaContextId');
    localStorage.removeItem('empresaContextTipo');
    localStorage.removeItem('empresaContextNome');
  }

  private finishTenantSelection(): void {
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('empresa-context-updated'));
    }
    this.loadMe();
    this.router.navigateByUrl('/home');
  }

  loadMe() {
    this.http.get(`${environment.apiBaseUrl}/api/me`).subscribe({
      next: data => this.me = data,
      error: err => this.me = { error: true, status: err.status, message: err.message }
    });
  }

  refreshTenants() {
    this.loadAllowedTenants();
  }

  loadAllowedTenants() {
    this.tenantService.allowed().subscribe({
      next: data => {
        const list = Array.isArray(data) ? data : ((data as any)?.content || []);
        this.allowedTenants = list;
        const hasSelected = this.allowedTenants.some(t => t.id === this.tenantId);
        if ((this.tenantId === null || !hasSelected) && this.allowedTenants.length >= 1 && this.allowedTenants[0]) {
          this.tenantId = this.allowedTenants[0].id;
          this.saveTenant();
        }
      },
      error: () => this.allowedTenants = []
    });
  }
}
