import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';

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
  templateUrl: './field-search.component.html',
  styleUrls: ['./field-search.component.css']
})
export class FieldSearchComponent {
  @Input() options: FieldSearchOption[] = [];
  @Input() label = 'Buscar';
  @Input() placeholder = 'Digite para buscar';
  @Input() defaultFields: string[] = [];
  @Output() searchChange = new EventEmitter<FieldSearchValue>();

  termControl = new FormControl('', { nonNullable: true });
  fieldsControl = new FormControl<string[]>([], { nonNullable: true });

  ngOnInit(): void {
    if (this.defaultFields.length) {
      this.fieldsControl.setValue([...this.defaultFields]);
    }
  }

  apply() {
    const term = this.termControl.value.trim();
    const fields = this.fieldsControl.value.length
      ? this.fieldsControl.value
      : this.options.map(o => o.key);
    this.searchChange.emit({ term, fields });
  }

  clear() {
    this.termControl.setValue('');
    this.fieldsControl.setValue(this.defaultFields.length ? [...this.defaultFields] : []);
    this.searchChange.emit({
      term: '',
      fields: this.fieldsControl.value.length ? this.fieldsControl.value : this.options.map(o => o.key)
    });
  }
}


