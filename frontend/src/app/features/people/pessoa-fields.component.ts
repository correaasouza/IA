import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

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
    MatSlideToggleModule
  ],
  templateUrl: './pessoa-fields.component.html',
  styleUrls: ['./pessoa-fields.component.css']
})
export class PessoaFieldsComponent {
  @Input({ required: true }) form!: FormGroup;
  @Input() rules: Record<string, PessoaFieldRule> = {};
  @Input() showStatus = true;

  labelFor(field: string, fallback: string): string {
    return this.rules[field]?.label || fallback;
  }

  isVisible(field: string): boolean {
    return this.rules[field]?.visible ?? true;
  }

  isRequired(field: string): boolean {
    return this.rules[field]?.required ?? false;
  }
}
