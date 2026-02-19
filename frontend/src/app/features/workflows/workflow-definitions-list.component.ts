import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { NotificationService } from '../../core/notifications/notification.service';
import { AccessControlDirective } from '../../shared/access-control.directive';
import { FeatureFlagService } from '../../core/features/feature-flag.service';
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
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    AccessControlDirective
  ],
  templateUrl: './workflow-definitions-list.component.html',
  styleUrls: ['./workflow-definitions-list.component.css']
})
export class WorkflowDefinitionsListComponent implements OnInit {
  canUseModule = true;
  cards: WorkflowOriginCard[] = [
    { origin: 'MOVIMENTO_ESTOQUE', definition: null, loading: true },
    { origin: 'ITEM_MOVIMENTO_ESTOQUE', definition: null, loading: true }
  ];

  constructor(
    private workflowService: WorkflowService,
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
    this.loadAll();
  }

  loadAll(): void {
    this.cards = this.cards.map(item => ({ ...item, loading: true }));
    for (const card of this.cards) {
      this.loadCard(card.origin);
    }
  }

  openConfiguration(card: WorkflowOriginCard): void {
    if (card.definition?.id) {
      this.router.navigate(['/configs/workflows', card.definition.id, 'edit']);
      return;
    }
    this.router.navigate(['/configs/workflows/new'], {
      queryParams: { origin: card.origin }
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

  originLabel(origin: WorkflowOrigin): string {
    return origin === 'ITEM_MOVIMENTO_ESTOQUE' ? 'Item de Movimento de Estoque' : 'Movimento de Estoque';
  }

  originHint(origin: WorkflowOrigin): string {
    return origin === 'ITEM_MOVIMENTO_ESTOQUE'
      ? 'Controla estados e transicoes dos itens do movimento.'
      : 'Controla estados e transicoes do cabecalho do movimento.';
  }

  private loadCard(origin: WorkflowOrigin): void {
    this.workflowService.getDefinitionByOrigin(origin).subscribe({
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
}
