import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-config-section-shell',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="config-section-shell rounded-xl border border-[var(--border)] bg-[var(--surface)] p-3 shadow-[var(--shadow-xs)]">
      <header class="mb-2.5 flex items-start justify-between gap-2" *ngIf="title || subtitle || hasActions">
        <div>
          <div class="text-sm font-semibold" *ngIf="title">{{ title }}</div>
          <div class="text-xs text-[var(--muted)]" *ngIf="subtitle">{{ subtitle }}</div>
        </div>
        <div class="config-section-shell-actions">
          <ng-content select="[shell-actions]"></ng-content>
        </div>
      </header>

      <ng-content></ng-content>
    </section>
  `,
  styles: [`
    :host {
      display: block;
      min-width: 0;
    }

    .config-section-shell-actions {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: 8px;
      min-width: 0;
    }
  `]
})
export class ConfigSectionShellComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() hasActions = false;
}
