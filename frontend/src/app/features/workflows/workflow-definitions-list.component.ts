import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { NotificationService } from '../../core/notifications/notification.service';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { FeatureFlagService } from '../../core/features/feature-flag.service';
import { MovementConfigService, MovimentoConfig } from '../movement-configs/movement-config.service';
import { WorkflowJsonImportDialogComponent } from './workflow-json-import-dialog.component';
import { WorkflowService } from './workflow.service';
import { WorkflowDefinition, WorkflowOrigin } from './models/workflow.models';

interface WorkflowOriginCard {
  origin: WorkflowOrigin;
  definition: WorkflowDefinition | null;
  loading: boolean;
}

@Component({
  selector: 'app-workflow-definitions-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
    AccessControlDirective
  ],
  templateUrl: './workflow-definitions-list.component.html',
  styleUrls: ['./workflow-definitions-list.component.css']
})
export class WorkflowDefinitionsListComponent implements OnInit {
  canUseModule = true;
  loadingConfigs = false;
  movimentoConfigs: MovimentoConfig[] = [];
  selectedMovimentoConfigId: number | null = null;
  cards: WorkflowOriginCard[] = [
    { origin: 'MOVIMENTO_ESTOQUE', definition: null, loading: true },
    { origin: 'ITEM_MOVIMENTO_ESTOQUE', definition: null, loading: true }
  ];

  constructor(
    private workflowService: WorkflowService,
    private movementConfigService: MovementConfigService,
    private featureFlagService: FeatureFlagService,
    private notify: NotificationService,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.canUseModule = this.featureFlagService.isEnabled('workflowEnabled', true);
    if (!this.canUseModule) {
      return;
    }
    this.loadMovimentoConfigs();
  }

  loadAll(): void {
    if (!this.selectedMovimentoConfigId) {
      this.cards = this.cards.map(item => ({ ...item, definition: null, loading: false }));
      return;
    }
    this.cards = this.cards.map(item => ({ ...item, loading: true }));
    for (const card of this.cards) {
      this.loadCard(card.origin);
    }
  }

  openConfiguration(card: WorkflowOriginCard): void {
    if (!this.selectedMovimentoConfigId) {
      this.notify.error('Selecione uma configuracao de movimento para continuar.');
      return;
    }
    if (card.definition?.id) {
      this.router.navigate(['/configs/workflows', card.definition.id, 'edit']);
      return;
    }
    this.router.navigate(['/configs/workflows/new'], {
      queryParams: {
        origin: card.origin,
        contextType: 'MOVIMENTO_CONFIG',
        contextId: this.selectedMovimentoConfigId
      }
    });
  }

  export(card: WorkflowOriginCard): void {
    const definitionId = Number(card.definition?.id || 0);
    if (!definitionId) {
      return;
    }
    this.workflowService.exportDefinition(definitionId).subscribe({
      next: payload => {
        const content = payload?.definitionJson || '';
        const blob = new Blob([content], { type: 'application/json;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `workflow-${card.origin.toLowerCase()}.json`;
        link.click();
        URL.revokeObjectURL(url);
      },
      error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel exportar o workflow.')
    });
  }

  importJson(): void {
    const ref = this.dialog.open(WorkflowJsonImportDialogComponent, {
      width: '980px',
      maxWidth: '96vw'
    });
    ref.afterClosed().subscribe((jsonText?: string) => {
      const normalized = (jsonText || '').trim();
      if (!normalized) {
        return;
      }
      this.workflowService.importDefinition({ definitionJson: normalized }).subscribe({
        next: created => {
          this.notify.success('Workflow importado.');
          this.router.navigate(['/configs/workflows', created.id, 'edit']);
        },
        error: err => this.notify.error(err?.error?.detail || 'Nao foi possivel importar o JSON.')
      });
    });
  }

  onMovimentoConfigChange(): void {
    this.loadAll();
  }

  originLabel(origin: WorkflowOrigin): string {
    return origin === 'ITEM_MOVIMENTO_ESTOQUE' ? 'Item de Movimento de Estoque' : 'Movimento de Estoque';
  }

  originHint(origin: WorkflowOrigin): string {
    return origin === 'ITEM_MOVIMENTO_ESTOQUE'
      ? 'Controla estados e transicoes dos itens do movimento.'
      : 'Controla estados e transicoes do cabecalho do movimento.';
  }

  private loadCard(origin: WorkflowOrigin): void {
    if (!this.selectedMovimentoConfigId) {
      this.updateCard(origin, null, false);
      return;
    }
    this.workflowService.getDefinitionByOrigin(origin, {
      type: 'MOVIMENTO_CONFIG',
      id: this.selectedMovimentoConfigId
    }).subscribe({
      next: definition => this.updateCard(origin, definition, false),
      error: err => {
        this.updateCard(origin, null, false);
        if (err?.status === 404) {
          return;
        }
        this.notify.error(err?.error?.detail || `Nao foi possivel carregar workflow da origem ${this.originLabel(origin)}.`);
      }
    });
  }

  private updateCard(origin: WorkflowOrigin, definition: WorkflowDefinition | null, loading: boolean): void {
    this.cards = this.cards.map(item =>
      item.origin === origin
        ? { ...item, definition, loading }
        : item);
  }

  private loadMovimentoConfigs(): void {
    this.loadingConfigs = true;
    this.movementConfigService.listByTipo('MOVIMENTO_ESTOQUE', 0, 500).subscribe({
      next: page => {
        this.loadingConfigs = false;
        this.movimentoConfigs = (page?.content || []).filter(item => item?.id && item.ativo !== false);
        this.selectedMovimentoConfigId = this.movimentoConfigs[0]?.id || null;
        this.loadAll();
      },
      error: err => {
        this.loadingConfigs = false;
        this.movimentoConfigs = [];
        this.selectedMovimentoConfigId = null;
        this.cards = this.cards.map(item => ({ ...item, definition: null, loading: false }));
        this.notify.error(err?.error?.detail || 'Nao foi possivel carregar configuracoes de movimento.');
      }
    });
  }
}
