import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { NotificationService } from '../../core/notifications/notification.service';
import { EntityTypeService, TipoEntidade } from '../entity-types/entity-type.service';
import { EntityRecordService, RegistroEntidadeContexto } from './entity-record.service';
import { EntityGroupsTreeComponent } from './entity-groups-tree.component';
import { EntityTypeAccessService } from './entity-type-access.service';

@Component({
  selector: 'app-entity-groups-page',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, EntityGroupsTreeComponent],
  templateUrl: './entity-groups-page.component.html',
  styleUrls: ['./entity-groups-page.component.css']
})
export class EntityGroupsPageComponent implements OnInit {
  tipoEntidadeId = 0;
  tipoNome = '';
  routeTipoSeed = '';
  routeCustomOnly = false;
  context: RegistroEntidadeContexto | null = null;
  contextWarning = '';
  loadingContext = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private typeService: EntityTypeService,
    private recordService: EntityRecordService,
    private notify: NotificationService,
    private typeAccess: EntityTypeAccessService
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      this.tipoEntidadeId = Number(params.get('tipoEntidadeId') || 0);
      this.routeTipoSeed = (params.get('tipoSeed') || '').trim().toUpperCase();
      const customOnlyValue = (params.get('customOnly') || '').trim().toLowerCase();
      this.routeCustomOnly = customOnlyValue === 'true' || customOnlyValue === '1';
      this.resolveTipoEntidadeAndLoad();
    });
  }

  pageTitle(): string {
    const nome = this.formatTipoNome(this.tipoNome);
    return nome ? `Grupos - ${nome}` : 'Grupos de Entidade';
  }

  backToEntities(): void {
    this.router.navigate(['/entities'], {
      queryParams: {
        tipoEntidadeId: this.tipoEntidadeId || null,
        tipoSeed: this.routeTipoSeed || null,
        customOnly: this.routeCustomOnly ? true : null
      }
    });
  }

  canManageGroups(): boolean {
    return !!this.tipoEntidadeId && !!this.context?.vinculado;
  }

  onGroupsChanged(): void {
    this.loadContext();
  }

  private resolveTipoEntidadeAndLoad(): void {
    if (this.tipoEntidadeId > 0) {
      this.loadTipoNomeAndContext();
      return;
    }

    this.typeService.list({ page: 0, size: 200, ativo: true }).subscribe({
      next: data => {
        const tipos = (data?.content || []).filter(item => item.ativo);
        if (!tipos.length) {
          this.context = null;
          this.contextWarning = 'Nenhum tipo de entidade ativo disponivel.';
          return;
        }
        let selected: TipoEntidade | undefined;
        if (this.routeTipoSeed) {
          selected = tipos.find(item => (item.codigoSeed || '').trim().toUpperCase() === this.routeTipoSeed);
        }
        if (!selected && this.routeCustomOnly) {
          selected = tipos.find(item => !item.tipoPadrao);
        }
        if (!selected) {
          selected = tipos[0];
        }
        if (!selected || !this.typeAccess.canAccessType(selected)) {
          this.context = null;
          this.contextWarning = 'Voce nao possui acesso aos tipos customizados disponiveis.';
          return;
        }
        this.tipoEntidadeId = selected?.id || 0;
        this.tipoNome = selected?.nome || '';
        this.loadContext();
      },
      error: () => {
        this.notify.error('Nao foi possivel carregar tipos de entidade.');
      }
    });
  }

  private loadTipoNomeAndContext(): void {
    this.typeService.get(this.tipoEntidadeId).subscribe({
      next: tipo => {
        this.tipoNome = tipo?.nome || '';
        if (!this.typeAccess.canAccessType(tipo)) {
          this.context = null;
          this.contextWarning = 'Voce nao possui acesso ao tipo de entidade selecionado.';
          return;
        }
        this.loadContext();
      },
      error: () => {
        this.tipoNome = '';
        if (!this.typeAccess.canAccessType({ id: this.tipoEntidadeId, codigoSeed: null })) {
          this.context = null;
          this.contextWarning = 'Voce nao possui acesso ao tipo de entidade selecionado.';
          return;
        }
        this.loadContext();
      }
    });
  }

  private loadContext(): void {
    if (!this.tipoEntidadeId) {
      this.context = null;
      this.contextWarning = 'Tipo de entidade nao informado.';
      return;
    }
    if (!this.hasEmpresaContext()) {
      this.context = null;
      this.contextWarning = 'Selecione uma empresa no topo do sistema para continuar.';
      return;
    }
    this.loadingContext = true;
    this.contextWarning = '';
    this.recordService.contextoEmpresa(this.tipoEntidadeId)
      .pipe(finalize(() => (this.loadingContext = false)))
      .subscribe({
        next: ctx => {
          this.context = ctx;
          if (!ctx.vinculado) {
            this.contextWarning = ctx.mensagem || 'Empresa selecionada sem grupo configurado para este tipo.';
            return;
          }
          this.contextWarning = '';
        },
        error: err => {
          this.context = null;
          this.contextWarning = err?.error?.detail || 'Nao foi possivel resolver o contexto da empresa.';
        }
      });
  }

  private formatTipoNome(value: string): string {
    const raw = (value || '').trim();
    if (!raw) return '';
    if (raw === raw.toUpperCase()) {
      return raw
        .toLowerCase()
        .split(' ')
        .filter(part => !!part)
        .map(part => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ');
    }
    return raw;
  }

  private hasEmpresaContext(): boolean {
    return !!(localStorage.getItem('empresaContextId') || '').trim();
  }
}
