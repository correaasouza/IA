import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSelectModule } from '@angular/material/select';

export interface PessoaFieldRule {
  visible?: boolean;
  required?: boolean;
  editable?: boolean;
  label?: string;
}

@Component({
  selector: 'app-pessoa-fields',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatSelectModule
  ],
  templateUrl: './pessoa-fields.component.html',
})
export class PessoaFieldsComponent {
  @Input({ required: true }) form!: FormGroup;
  @Input() rules: Record<string, PessoaFieldRule> = {};
  @Input() showStatus = true;

  labelFor(field: string, fallback: string): string {
    return this.rules[field]?.label || fallback;
  }

  getTipoPessoa(): 'FISICA' | 'JURIDICA' | 'ESTRANGEIRA' {
    const value = (this.form.get('tipoPessoa')?.value as string) || 'FISICA';
    if (value === 'JURIDICA' || value === 'ESTRANGEIRA') return value;
    return 'FISICA';
  }

  isVisible(field: string): boolean {
    return this.rules[field]?.visible ?? true;
  }

  isDocVisible(field: 'cpf' | 'cnpj' | 'idEstrangeiro'): boolean {
    const tipo = this.getTipoPessoa();
    if (field === 'cpf') return tipo === 'FISICA';
    if (field === 'cnpj') return tipo === 'JURIDICA';
    return tipo === 'ESTRANGEIRA';
  }

  isRequired(field: string): boolean {
    return this.rules[field]?.required ?? false;
  }
}

