import { AbstractControl, ValidationErrors } from '@angular/forms';

function onlyDigits(value: string): string {
  return value.replace(/\D/g, '');
}

function isValidCpf(cpf: string): boolean {
  if (cpf.length !== 11 || /^([0-9])\1+$/.test(cpf)) return false;
  let sum = 0;
  for (let i = 0; i < 9; i++) sum += parseInt(cpf.charAt(i), 10) * (10 - i);
  let dv = (sum * 10) % 11;
  if (dv === 10) dv = 0;
  if (dv !== parseInt(cpf.charAt(9), 10)) return false;
  sum = 0;
  for (let i = 0; i < 10; i++) sum += parseInt(cpf.charAt(i), 10) * (11 - i);
  dv = (sum * 10) % 11;
  if (dv === 10) dv = 0;
  return dv === parseInt(cpf.charAt(10), 10);
}

function isValidCnpj(cnpj: string): boolean {
  if (cnpj.length !== 14 || /^([0-9])\1+$/.test(cnpj)) return false;
  const weights1 = [5,4,3,2,9,8,7,6,5,4,3,2];
  const weights2 = [6,5,4,3,2,9,8,7,6,5,4,3,2];
  let sum = 0;
  for (let i = 0; i < 12; i++) sum += parseInt(cnpj.charAt(i), 10) * (weights1[i] ?? 0);
  let dv = sum % 11;
  dv = dv < 2 ? 0 : 11 - dv;
  if (dv !== parseInt(cnpj.charAt(12), 10)) return false;
  sum = 0;
  for (let i = 0; i < 13; i++) sum += parseInt(cnpj.charAt(i), 10) * (weights2[i] ?? 0);
  dv = sum % 11;
  dv = dv < 2 ? 0 : 11 - dv;
  return dv === parseInt(cnpj.charAt(13), 10);
}

export function cpfCnpjValidator(control: AbstractControl): ValidationErrors | null {
  const value = (control.value || '').toString().trim();
  if (!value) return null;
  const digits = onlyDigits(value);
  const valid = digits.length === 11 ? isValidCpf(digits) : isValidCnpj(digits);
  return valid ? null : { cpfCnpj: true };
}

export function formatCpfCnpj(value: string): string {
  const digits = onlyDigits(value);
  if (digits.length <= 11) {
    return digits
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
  }
  return digits
    .replace(/(\d{2})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1/$2')
    .replace(/(\d{4})(\d{1,2})$/, '$1-$2');
}
