import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';

import { ConfigService } from './config.service';

@Component({
  selector: 'app-configs',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatSelectModule, MatIconModule],
  templateUrl: './configs.component.html'
})
export class ConfigsComponent {
  statusColunas: 'idle' | 'ok' | 'error' = 'idle';
  statusForm: 'idle' | 'ok' | 'error' = 'idle';
  formColunas = this.fb.group({
    screenId: ['', Validators.required],
    scopeTipo: ['TENANT', Validators.required],
    scopeValor: [''],
    configJson: ['{}', Validators.required]
  });

  formForm = this.fb.group({
    screenId: ['', Validators.required],
    scopeTipo: ['TENANT', Validators.required],
    scopeValor: [''],
    configJson: ['{}', Validators.required]
  });

  constructor(private fb: FormBuilder, private service: ConfigService) {}

  saveColunas() {
    if (this.formColunas.invalid) {
      return;
    }
    this.statusColunas = 'idle';
    this.service.saveColunas(this.formColunas.value).subscribe({
      next: () => this.statusColunas = 'ok',
      error: () => this.statusColunas = 'error'
    });
  }

  saveForm() {
    if (this.formForm.invalid) {
      return;
    }
    this.statusForm = 'idle';
    this.service.saveForm(this.formForm.value).subscribe({
      next: () => this.statusForm = 'ok',
      error: () => this.statusForm = 'error'
    });
  }
}
