import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CatalogCrudType, CatalogItemContext, CatalogItemService } from './catalog-item.service';
import { CatalogGroupsTreeComponent } from './catalog-groups-tree.component';

@Component({
  selector: 'app-catalog-groups-page',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, CatalogGroupsTreeComponent],
  templateUrl: './catalog-groups-page.component.html',
  styleUrls: ['./catalog-groups-page.component.css']
})
export class CatalogGroupsPageComponent implements OnInit {
  type: CatalogCrudType = 'PRODUCTS';
  title = 'Produtos';

  context: CatalogItemContext | null = null;
  contextWarning = '';
  loadingContext = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private itemService: CatalogItemService
  ) {}

  ngOnInit(): void {
    this.route.data.subscribe(data => {
      this.type = (data['type'] || 'PRODUCTS') as CatalogCrudType;
      this.title = data['title'] || (this.type === 'PRODUCTS' ? 'Produtos' : 'Servicos');
      this.loadContext();
    });
  }

  pageTitle(): string {
    return `Grupos de ${this.title}`;
  }

  backToList(): void {
    this.router.navigate([`/catalog/${this.routeSegment()}`]);
  }

  onGroupsChanged(): void {
    this.loadContext();
  }

  private loadContext(): void {
    if (!this.hasEmpresaContext()) {
      this.context = null;
      this.contextWarning = 'Selecione uma empresa no topo do sistema para continuar.';
      return;
    }

    this.loadingContext = true;
    this.contextWarning = '';
    this.itemService.contextoEmpresa(this.type)
      .pipe(finalize(() => (this.loadingContext = false)))
      .subscribe({
        next: context => {
          this.context = context;
          if (!context.vinculado) {
            this.contextWarning = context.mensagem || 'Empresa sem grupo configurado para este catalogo.';
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

  private routeSegment(): string {
    return this.type === 'PRODUCTS' ? 'products' : 'services';
  }

  private hasEmpresaContext(): boolean {
    return !!(localStorage.getItem('empresaContextId') || '').trim();
  }
}
