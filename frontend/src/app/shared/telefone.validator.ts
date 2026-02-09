import { AbstractControl, ValidationErrors } from '@angular/forms';

const PHONE_REGEX = /^(\(?\d{2}\)?\s?)?\d{4,5}-?\d{4}$/;

export function telefoneValidator(control: AbstractControl): ValidationErrors | null {
  const value = (control.value || '').toString().trim();
  if (!value) return null;
  return PHONE_REGEX.test(value) ? null : { telefone: true };
}

export function formatTelefone(value: string): string {
  const digits = value.replace(/\D/g, '');
  if (digits.length <= 10) {
    return digits
      .replace(/(\d{2})(\d)/, '($1) $2')
      .replace(/(\d{4})(\d)/, '$1-$2');
  }
  return digits
    .replace(/(\d{2})(\d)/, '($1) $2')
    .replace(/(\d{5})(\d)/, '$1-$2');
}
