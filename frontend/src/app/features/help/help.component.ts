import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-help',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatButtonModule, MatChipsModule, RouterLink],
  template: `
    <div class="help-grid">
      <mat-card class="card hero">
        <div class="hero-content">
          <div class="hero-title">
            <mat-icon>support_agent</mat-icon>
            Central de Ajuda
          </div>
          <div class="hero-sub">Guia rapido das funcionalidades e como usar o sistema.</div>
          <div class="hero-actions">
            <button mat-flat-button color="primary" routerLink="/">Ir para Home</button>
            <button mat-stroked-button routerLink="/reports">Relatorios</button>
          </div>
        </div>
      </mat-card>

      <mat-card class="card">
        <mat-card-title>Fluxo inicial</mat-card-title>
        <mat-card-content>
          <div class="step">
            <div class="step-num">1</div>
            <div>
              <div class="step-title">Selecione o locatario</div>
              <div class="step-desc">No login, escolha o locatario permitido. Isso define o contexto do X-Tenant-Id.</div>
            </div>
          </div>
          <div class="step">
            <div class="step-num">2</div>
            <div>
              <div class="step-title">Cadastre tipos de entidade</div>
              <div class="step-desc">Defina Cliente, Fornecedor, Funcionario e os campos obrigatorios.</div>
            </div>
          </div>
          <div class="step">
            <div class="step-num">3</div>
            <div>
              <div class="step-title">Cadastre registros</div>
              <div class="step-desc">Use a tela de Cadastros para inserir registros e contatos.</div>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <mat-card class="card">
        <mat-card-title>Funcoes principais</mat-card-title>
        <mat-card-content>
          <div class="feature">
            <div class="feature-title"><mat-icon>domain</mat-icon> Locatarios</div>
            <div class="feature-desc">Criar, editar, ativar/desativar, renovar vencimento e acompanhar status.</div>
            <mat-chip-set>
              <mat-chip>MASTER_ADMIN</mat-chip>
            </mat-chip-set>
          </div>
          <div class="feature">
            <div class="feature-title"><mat-icon>group</mat-icon> Usuarios</div>
            <div class="feature-desc">Criar usuarios, desativar, resetar senha e vincular papeis.</div>
            <mat-chip-set>
              <mat-chip>MASTER_ADMIN</mat-chip>
              <mat-chip>TENANT_ADMIN</mat-chip>
            </mat-chip-set>
          </div>
          <div class="feature">
            <div class="feature-title"><mat-icon>security</mat-icon> Papeis e permissoes</div>
            <div class="feature-desc">Definir papeis por locatario, configurar permissoes e atribuicoes.</div>
            <mat-chip-set>
              <mat-chip>MASTER_ADMIN</mat-chip>
              <mat-chip>TENANT_ADMIN</mat-chip>
            </mat-chip-set>
          </div>
          <div class="feature">
            <div class="feature-title"><mat-icon>assignment</mat-icon> Cadastros</div>
            <div class="feature-desc">CRUD de registros por tipo de entidade. Suporta contatos e validacoes.</div>
            <mat-chip-set>
              <mat-chip>MASTER_ADMIN</mat-chip>
              <mat-chip>TENANT_ADMIN</mat-chip>
              <mat-chip>USER</mat-chip>
            </mat-chip-set>
          </div>
          <div class="feature">
            <div class="feature-title"><mat-icon>view_list</mat-icon> Metadados</div>
            <div class="feature-desc">Configurar tipos de entidade, campos e validacoes.</div>
            <mat-chip-set>
              <mat-chip>MASTER_ADMIN</mat-chip>
              <mat-chip>TENANT_ADMIN</mat-chip>
            </mat-chip-set>
          </div>
          <div class="feature">
            <div class="feature-title"><mat-icon>settings</mat-icon> Configuracoes</div>
            <div class="feature-desc">Ajustar colunas e formularios por usuario, papel e locatario.</div>
            <mat-chip-set>
              <mat-chip>CONFIG_EDITOR</mat-chip>
            </mat-chip-set>
          </div>
          <div class="feature">
            <div class="feature-title"><mat-icon>bar_chart</mat-icon> Relatorios</div>
            <div class="feature-desc">Visao geral, comparativos, exportacao CSV/XLSX.</div>
            <mat-chip-set>
              <mat-chip>RELATORIO_VIEW</mat-chip>
            </mat-chip-set>
          </div>
        </mat-card-content>
      </mat-card>

      <mat-card class="card">
        <mat-card-title>Dicas rapidas</mat-card-title>
        <mat-card-content>
          <div class="tip"><mat-icon>bolt</mat-icon> Use atalhos no topo para acessar telas frequentes.</div>
          <div class="tip"><mat-icon>filter_alt</mat-icon> Utilize filtros e ordenacao nas listagens.</div>
          <div class="tip"><mat-icon>lock</mat-icon> Se o locatario estiver bloqueado, contate o administrador.</div>
          <div class="tip"><mat-icon>verified</mat-icon> Campos obrigatorios sao validados automaticamente.</div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .help-grid {
        display: grid;
        gap: 12px;
        grid-template-columns: repeat(2, minmax(280px, 1fr));
      }
      .hero {
        grid-column: 1 / -1;
        background: linear-gradient(135deg, rgba(11, 61, 145, 0.08), transparent);
      }
      .hero-title {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        font-size: 18px;
        font-weight: 600;
      }
      .hero-sub {
        color: var(--muted);
        margin-top: 4px;
      }
      .hero-actions {
        margin-top: 10px;
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
      }
      .step {
        display: grid;
        grid-template-columns: 28px 1fr;
        gap: 10px;
        padding: 6px 0;
      }
      .step-num {
        width: 28px;
        height: 28px;
        border-radius: 8px;
        background: #0b3d91;
        color: white;
        display: grid;
        place-items: center;
        font-weight: 600;
        font-size: 12px;
      }
      .step-title { font-weight: 600; }
      .step-desc { color: var(--muted); font-size: 12px; }
      .feature { padding: 8px 0; border-bottom: 1px dashed var(--border); }
      .feature:last-child { border-bottom: none; }
      .feature-title { display: inline-flex; align-items: center; gap: 6px; font-weight: 600; }
      .feature-desc { color: var(--muted); font-size: 12px; margin: 4px 0 6px; }
      .tip { display: inline-flex; align-items: center; gap: 8px; padding: 6px 0; color: var(--muted); }
      @media (max-width: 900px) {
        .help-grid { grid-template-columns: 1fr; }
      }
    `
  ]
})
export class HelpComponent {}
