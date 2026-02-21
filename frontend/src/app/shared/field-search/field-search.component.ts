import { Component, EventEmitter, Input, Output, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { combineLatest, Subject } from 'rxjs';
import { debounceTime, startWith, takeUntil } from 'rxjs/operators';

export interface FieldSearchOption {
  key: string;
  label: string;
}

export interface FieldSearchValue {
  term: string;
  fields: string[];
}

@Component({
  selector: 'app-field-search',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './field-search.component.html',
  styleUrls: ['./field-search.component.css']
})
export class FieldSearchComponent implements OnInit, OnDestroy {
  @Input() options: FieldSearchOption[] = [];
  @Input() label = 'Buscar';
  @Input() placeholder = 'Digite para buscar';
  @Input() defaultFields: string[] = [];
  @Input() allOptionLabel = 'pesquisa completa';
  @Input() initialTerm = '';
  @Input() initialScope: string | null = null;
  @Input() showActions = true;
  @Input() actionLabel = 'Buscar';
  @Output() searchChange = new EventEmitter<FieldSearchValue>();

  termControl = new FormControl('', { nonNullable: true });
  fieldScopeControl = new FormControl('all', { nonNullable: true });

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    if (!this.defaultFields.length) {
      this.defaultFields = this.options.map(o => o.key);
    }
    const normalizedInitialTerm = (this.initialTerm || '').trim();
    if (normalizedInitialTerm) {
      this.termControl.setValue(normalizedInitialTerm, { emitEvent: false });
    }

    const normalizedInitialScope = (this.initialScope || '').trim();
    const hasInitialScope = !!normalizedInitialScope
      && (normalizedInitialScope === 'all' || this.options.some(option => option.key === normalizedInitialScope));
    if (hasInitialScope) {
      this.fieldScopeControl.setValue(normalizedInitialScope, { emitEvent: false });
    } else if (this.defaultFields.length === 1) {
      this.fieldScopeControl.setValue(this.defaultFields[0]!, { emitEvent: false });
    }

    if (!this.showActions) {
      combineLatest([
        this.termControl.valueChanges.pipe(startWith(this.termControl.value)),
        this.fieldScopeControl.valueChanges.pipe(startWith(this.fieldScopeControl.value))
      ])
        .pipe(debounceTime(200), takeUntil(this.destroy$))
        .subscribe(([term, scope]) => this.emitChange(term, this.resolveFields(scope)));
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  apply() {
    this.emitChange(this.termControl.value, this.resolveFields(this.fieldScopeControl.value));
  }

  clear() {
    this.termControl.setValue('');
    this.fieldScopeControl.setValue(this.defaultFields.length === 1 ? this.defaultFields[0]! : 'all');
    this.emitChange('', this.resolveFields(this.fieldScopeControl.value));
  }

  private emitChange(term: string, fields: string[]) {
    const normalizedTerm = (term || '').trim();
    const normalizedFields = fields.length ? fields : this.options.map(o => o.key);
    this.searchChange.emit({ term: normalizedTerm, fields: normalizedFields });
  }

  private resolveFields(scope: string): string[] {
    if (!scope || scope === 'all') {
      return this.options.map(o => o.key);
    }
    return [scope];
  }
}
