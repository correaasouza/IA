import { Component, EventEmitter, Input, Output, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
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
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule
  ],
  templateUrl: './field-search.component.html'
})
export class FieldSearchComponent implements OnInit, OnDestroy {
  @Input() options: FieldSearchOption[] = [];
  @Input() label = 'Buscar';
  @Input() placeholder = 'Digite para buscar';
  @Input() defaultFields: string[] = [];
  @Input() showActions = true;
  @Input() actionLabel = 'Buscar';
  @Output() searchChange = new EventEmitter<FieldSearchValue>();

  termControl = new FormControl('', { nonNullable: true });
  fieldsControl = new FormControl<string[]>([], { nonNullable: true });

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    if (this.defaultFields.length) {
      this.fieldsControl.setValue([...this.defaultFields]);
    }

    if (!this.showActions) {
      combineLatest([
        this.termControl.valueChanges.pipe(startWith(this.termControl.value)),
        this.fieldsControl.valueChanges.pipe(startWith(this.fieldsControl.value))
      ])
        .pipe(debounceTime(200), takeUntil(this.destroy$))
        .subscribe(([term, fields]) => this.emitChange(term, fields));
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  apply() {
    this.emitChange(this.termControl.value, this.fieldsControl.value);
  }

  clear() {
    this.termControl.setValue('');
    this.fieldsControl.setValue(this.defaultFields.length ? [...this.defaultFields] : []);
    this.emitChange('', this.fieldsControl.value);
  }

  private emitChange(term: string, fields: string[]) {
    const normalizedTerm = (term || '').trim();
    const normalizedFields = fields.length ? fields : this.options.map(o => o.key);
    this.searchChange.emit({ term: normalizedTerm, fields: normalizedFields });
  }
}
