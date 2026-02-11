import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { DateMaskDirective } from '../../shared/date-mask.directive';
import { finalize } from 'rxjs/operators';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';

import { RelatorioService } from './relatorio.service';
import { TipoEntidadeService, TipoEntidade } from '../entities/tipo-entidade.service';
import { environment } from '../../../environments/environment';
import { toIsoDate } from '../../shared/date-utils';

@Component({
  selector: 'app-reports',
  standalone: true,
imports: [CommonModule, MatTableModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule, FormsModule, DateMaskDirective, InlineLoaderComponent],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.css']
})
export class ReportsComponent implements OnInit {
  entidades: any[] = [];
  comparativo: any[] = [];
  contatos: any[] = [];
  locatarios: any = null;
  pendencias: any[] = [];
  entColumns = ['nome', 'total'];
  compColumns = ['nome', 'p1', 'p2', 'delta'];
  contColumns = ['tipo', 'total'];
  pendColumns = ['entidade', 'tipoContato'];

  tipos: TipoEntidade[] = [];
  tipoEntidadeId: number | null = null;
  criadoDe = '';
  criadoAte = '';
  criadoDe1 = '';
  criadoAte1 = '';
  criadoDe2 = '';
  criadoAte2 = '';

  maxEntidades = 0;
  maxContatos = 0;
  loadingEntidades = false;
  loadingComparativo = false;

  constructor(private service: RelatorioService, private tipoService: TipoEntidadeService) {}

  ngOnInit(): void {
    this.tipoService.list(0, 50).subscribe({ next: data => this.tipos = data.content || [] });
    this.loadEntidades();
    this.loadComparativo();
    this.service.contatos().subscribe({ next: data => { this.contatos = data; this.maxContatos = this.max(data); } });
    this.service.locatarios().subscribe({ next: data => this.locatarios = data });
    this.service.pendenciasContato().subscribe({ next: data => this.pendencias = data });
  }

  loadEntidades() {
    this.loadingEntidades = true;
    this.service.entidades({
      tipoEntidadeId: this.tipoEntidadeId || undefined,
      criadoDe: this.criadoDe ? toIsoDate(this.criadoDe) : undefined,
      criadoAte: this.criadoAte ? toIsoDate(this.criadoAte) : undefined
    }).pipe(finalize(() => this.loadingEntidades = false)).subscribe({ next: data => { this.entidades = data; this.maxEntidades = this.max(data); } });
  }

  downloadEntidadesCsv() {
    const qs = new URLSearchParams();
    if (this.tipoEntidadeId) qs.set('tipoEntidadeId', String(this.tipoEntidadeId));
    if (this.criadoDe) qs.set('criadoDe', toIsoDate(this.criadoDe));
    if (this.criadoAte) qs.set('criadoAte', toIsoDate(this.criadoAte));
    const q = qs.toString();
    window.open(`${environment.apiBaseUrl}/api/relatorios/entidades.csv${q ? '?' + q : ''}`, '_blank');
  }

  downloadEntidadesXlsx() {
    const qs = new URLSearchParams();
    if (this.tipoEntidadeId) qs.set('tipoEntidadeId', String(this.tipoEntidadeId));
    if (this.criadoDe) qs.set('criadoDe', toIsoDate(this.criadoDe));
    if (this.criadoAte) qs.set('criadoAte', toIsoDate(this.criadoAte));
    const q = qs.toString();
    window.open(`${environment.apiBaseUrl}/api/relatorios/entidades.xlsx${q ? '?' + q : ''}`, '_blank');
  }

  downloadEntidadesPdf() {
    const qs = new URLSearchParams();
    if (this.tipoEntidadeId) qs.set('tipoEntidadeId', String(this.tipoEntidadeId));
    if (this.criadoDe) qs.set('criadoDe', toIsoDate(this.criadoDe));
    if (this.criadoAte) qs.set('criadoAte', toIsoDate(this.criadoAte));
    const q = qs.toString();
    window.open(`${environment.apiBaseUrl}/api/relatorios/entidades.pdf${q ? '?' + q : ''}`, '_blank');
  }

  downloadContatosCsv() {
    window.open(`${environment.apiBaseUrl}/api/relatorios/contatos.csv`, '_blank');
  }

  downloadContatosXlsx() {
    window.open(`${environment.apiBaseUrl}/api/relatorios/contatos.xlsx`, '_blank');
  }

  downloadContatosPdf() {
    window.open(`${environment.apiBaseUrl}/api/relatorios/contatos.pdf`, '_blank');
  }

  downloadPendenciasCsv() {
    window.open(`${environment.apiBaseUrl}/api/relatorios/pendencias-contato.csv`, '_blank');
  }

  downloadPendenciasXlsx() {
    window.open(`${environment.apiBaseUrl}/api/relatorios/pendencias-contato.xlsx`, '_blank');
  }

  downloadPendenciasPdf() {
    window.open(`${environment.apiBaseUrl}/api/relatorios/pendencias-contato.pdf`, '_blank');
  }

  loadComparativo() {
    this.loadingComparativo = true;
    this.service.entidadesComparativo({
      criadoDe1: this.criadoDe1 ? toIsoDate(this.criadoDe1) : undefined,
      criadoAte1: this.criadoAte1 ? toIsoDate(this.criadoAte1) : undefined,
      criadoDe2: this.criadoDe2 ? toIsoDate(this.criadoDe2) : undefined,
      criadoAte2: this.criadoAte2 ? toIsoDate(this.criadoAte2) : undefined
    }).pipe(finalize(() => this.loadingComparativo = false)).subscribe({ next: data => this.comparativo = data || [] });
  }

  downloadComparativoXlsx() {
    const qs = new URLSearchParams();
    if (this.criadoDe1) qs.set('criadoDe1', toIsoDate(this.criadoDe1));
    if (this.criadoAte1) qs.set('criadoAte1', toIsoDate(this.criadoAte1));
    if (this.criadoDe2) qs.set('criadoDe2', toIsoDate(this.criadoDe2));
    if (this.criadoAte2) qs.set('criadoAte2', toIsoDate(this.criadoAte2));
    const q = qs.toString();
    window.open(`${environment.apiBaseUrl}/api/relatorios/entidades-comparativo.xlsx${q ? '?' + q : ''}`, '_blank');
  }

  downloadComparativoPdf() {
    const qs = new URLSearchParams();
    if (this.criadoDe1) qs.set('criadoDe1', toIsoDate(this.criadoDe1));
    if (this.criadoAte1) qs.set('criadoAte1', toIsoDate(this.criadoAte1));
    if (this.criadoDe2) qs.set('criadoDe2', toIsoDate(this.criadoDe2));
    if (this.criadoAte2) qs.set('criadoAte2', toIsoDate(this.criadoAte2));
    const q = qs.toString();
    window.open(`${environment.apiBaseUrl}/api/relatorios/entidades-comparativo.pdf${q ? '?' + q : ''}`, '_blank');
  }

  max(rows: any[]): number {
    if (!rows || rows.length === 0) return 0;
    return Math.max(...rows.map(r => r.total || 0));
  }

  barWidth(value: number, max: number): number {
    if (!max) return 0;
    return Math.round((value / max) * 100);
  }
}
