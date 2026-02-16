import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { finalize } from 'rxjs/operators';
import { NotificationService } from '../../core/notifications/notification.service';
import { InlineLoaderComponent } from '../../shared/inline-loader.component';
import {
  CatalogCrudType,
  CatalogItem,
  CatalogItemContext,
  CatalogItemPayload,
  CatalogItemService
} from './catalog-item.service';
import { CatalogGroupNode, CatalogGroupService } from './catalog-group.service';

@Component({
  selector: 'app-catalog-item-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    InlineLoaderComponent
  ],
  templateUrl: './catalog-item-form.component.html',
  styleUrls: ['./catalog-item-form.component.css']
})
export class CatalogItemFormComponent implements OnInit {
  mode: 'new' | 'view' | 'edit' = 'new';
  type: CatalogCrudType = 'PRODUCTS';
  titlePlural = 'Produtos';
  titleSingular = 'produto';

  itemId: number | null = null;
  context: CatalogItemContext | null = null;
  contextWarning = '';

  groups: Array<{ id: number; nome: string }> = [];

  loading = false;
  saving = false;
  deleting = false;
  codigoInfo = 'Gerado ao salvar';

  form = this.fb.group({
    codigo: [null as number | null],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    descricao: ['', [Validators.maxLength(255)]],
    catalogGroupId: [null as number | null],
    ativo: [true, Validators.required]
  });

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private itemService: CatalogItemService,
    private groupService: CatalogGroupService,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.route.data.subscribe(data => {
      this.type = (data['type'] || 'PRODUCTS') as CatalogCrudType;
      this.titlePlural = data['title'] || (this.type === 'PRODUCTS' ? 'Produtos' : 'Servicos');
      this.titleSingular = data['singular'] || (this.type === 'PRODUCTS' ? 'produto' : 'servico');
      this.resolveMode();
      this.loadContextAndData();
    });
  }

  toEdit(): void {
    if (!this.itemId) return;
    this.router.navigate([`/catalog/${this.routeSegment()}/${this.itemId}/edit`]);
  }

  back(): void {
    this.router.navigate([`/catalog/${this.routeSegment()}`]);
  }

  save(): void {
    if (this.mode === 'view' || !this.context?.vinculado) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.buildPayload();
    this.saving = true;

    if (this.mode === 'new') {
      this.itemService.create(this.type, payload)
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: created => {
            this.notify.success(`${this.titleLabel()} criado.`);
            this.router.navigate([`/catalog/${this.routeSegment()}/${created.id}`]);
          },
          error: err => this.notify.error(err?.error?.detail || `Nao foi possivel criar ${this.titleLabelLower()}.`)
        });
      return;
    }

    if (!this.itemId) {
      this.saving = false;
      return;
    }

    this.itemService.update(this.type, this.itemId, payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: updated => {
          this.notify.success(`${this.titleLabel()} atualizado.`);
          this.router.navigate([`/catalog/${this.routeSegment()}/${updated.id}`]);
        },
        error: err => this.notify.error(err?.error?.detail || `Nao foi possivel atualizar ${this.titleLabelLower()}.`)
      });
  }

  remove(): void {
    if (!this.itemId || this.mode === 'new') return;
    if (!confirm(`Excluir ${this.titleLabelLower()} codigo ${this.codigoInfo}?`)) return;

    this.deleting = true;
    this.itemService.delete(this.type, this.itemId)
      .pipe(finalize(() => (this.deleting = false)))
      .subscribe({
        next: () => {
          this.notify.success(`${this.titleLabel()} excluido.`);
          this.back();
        },
        error: err => this.notify.error(err?.error?.detail || `Nao foi possivel excluir ${this.titleLabelLower()}.`)
      });
  }

  setAtivoFromHeader(nextValue: boolean): void {
    this.form.controls.ativo.setValue(!!nextValue);
  }

  ativoHeaderLabel(): string {
    return this.form.value.ativo ? 'Ativo' : 'Inativo';
  }

  cadastroTitle(): string {
    return `Cadastro de ${this.titlePlural}`;
  }

  subtitle(): string {
    if (this.mode === 'new') return `Novo ${this.titleSingular}`;
    if (this.mode === 'edit') return `Editar ${this.titleSingular}`;
    return `Consultar ${this.titleSingular}`;
  }

  codigoHint(): string {
    if (this.context?.numberingMode === 'AUTOMATICA') {
      return this.mode === 'new' ? 'Gerado automaticamente ao salvar.' : 'Codigo gerado automaticamente.';
    }
    return 'Codigo informado manualmente.';
  }

  private resolveMode(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const isEdit = this.route.snapshot.url.some(segment => segment.path === 'edit');

    if (!idParam) {
      this.mode = 'new';
      this.itemId = null;
      this.codigoInfo = 'Gerado ao salvar';
      return;
    }

    this.itemId = Number(idParam);
    this.mode = isEdit ? 'edit' : 'view';
  }

  private loadContextAndData(): void {
    if (!this.hasEmpresaContext()) {
      this.context = null;
      this.contextWarning = 'Selecione uma empresa no topo do sistema para continuar.';
      this.form.disable();
      return;
    }

    this.loading = true;
    this.itemService.contextoEmpresa(this.type)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: context => {
          this.context = context;
          if (!context.vinculado) {
            this.contextWarning = context.mensagem || 'Empresa sem grupo configurado para este catalogo.';
            this.form.disable();
            return;
          }

          this.contextWarning = '';
          this.loadGroups();
          this.applyNumberingMode();

          if (this.itemId) {
            this.loadItem(this.itemId);
          } else {
            this.applyModeState();
          }
        },
        error: err => {
          this.context = null;
          this.contextWarning = err?.error?.detail || 'Nao foi possivel resolver o contexto da empresa.';
          this.form.disable();
        }
      });
  }

  private loadItem(id: number): void {
    this.loading = true;
    this.itemService.get(this.type, id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: item => {
          this.patchForm(item);
          this.applyModeState();
        },
        error: err => {
          this.notify.error(err?.error?.detail || 'Nao foi possivel carregar item do catalogo.');
          this.back();
        }
      });
  }

  private patchForm(item: CatalogItem): void {
    this.codigoInfo = String(item.codigo || '');
    this.form.patchValue({
      codigo: item.codigo,
      nome: item.nome,
      descricao: item.descricao || '',
      catalogGroupId: item.catalogGroupId || null,
      ativo: !!item.ativo
    });
  }

  private applyModeState(): void {
    if (!this.context?.vinculado) {
      this.form.disable();
      return;
    }

    if (this.mode === 'view') {
      this.form.disable();
      return;
    }

    this.form.enable();
    this.applyNumberingMode();
  }

  private applyNumberingMode(): void {
    const codigoControl = this.form.controls.codigo;
    if (this.context?.numberingMode === 'MANUAL') {
      codigoControl.addValidators([Validators.required, Validators.min(1)]);
      if (this.mode !== 'view') {
        codigoControl.enable({ emitEvent: false });
      }
    } else {
      codigoControl.clearValidators();
      if (this.mode === 'new') {
        codigoControl.setValue(null, { emitEvent: false });
      }
      codigoControl.disable({ emitEvent: false });
    }
    codigoControl.updateValueAndValidity({ emitEvent: false });
  }

  private loadGroups(): void {
    this.groupService.tree(this.type).subscribe({
      next: tree => {
        this.groups = this.flatten(tree || []);
      },
      error: () => {
        this.groups = [];
      }
    });
  }

  private flatten(
    nodes: CatalogGroupNode[],
    acc: Array<{ id: number; nome: string }> = [],
    prefix = ''
  ): Array<{ id: number; nome: string }> {
    for (const node of nodes || []) {
      const label = prefix ? `${prefix} / ${node.nome}` : node.nome;
      acc.push({ id: node.id, nome: label });
      this.flatten(node.children || [], acc, label);
    }
    return acc;
  }

  private buildPayload(): CatalogItemPayload {
    const codigoValue = Number(this.form.value.codigo || 0);
    const codigo = this.context?.numberingMode === 'MANUAL' && Number.isFinite(codigoValue) && codigoValue > 0
      ? codigoValue
      : null;

    return {
      codigo,
      nome: (this.form.value.nome || '').trim(),
      descricao: (this.form.value.descricao || '').trim() || null,
      catalogGroupId: this.toPositive(this.form.value.catalogGroupId),
      ativo: !!this.form.value.ativo
    };
  }

  private toPositive(value: unknown): number | null {
    const parsed = Number(value || 0);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  private routeSegment(): string {
    return this.type === 'PRODUCTS' ? 'products' : 'services';
  }

  private titleLabel(): string {
    return this.type === 'PRODUCTS' ? 'Produto' : 'Servico';
  }

  private titleLabelLower(): string {
    return this.type === 'PRODUCTS' ? 'produto' : 'servico';
  }

  private hasEmpresaContext(): boolean {
    return !!(localStorage.getItem('empresaContextId') || '').trim();
  }
}
