import { AbstractControl, ValidationErrors } from '@angular/forms';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^(\(?\d{2}\)?\s?)?\d{4,5}-?\d{4}$/;

export function contatoValorValidator(tipoProvider: () => string | null, regexProvider?: () => string | null) {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = (control.value || '').toString().trim();
    if (!value) return null;
    const regex = regexProvider ? (regexProvider() || '') : '';
    if (regex) {
      try {
        const re = new RegExp(regex);
        return re.test(value) ? null : { regex: true };
      } catch {
        return null;
      }
    }
    const tipo = (tipoProvider() || '').toUpperCase();
    if (tipo === 'EMAIL') {
      return EMAIL_REGEX.test(value) ? null : { email: true };
    }
    if (tipo === 'TELEFONE' || tipo === 'WHATSAPP') {
      return PHONE_REGEX.test(value) ? null : { telefone: true };
    }
    return null;
  };
}

export function contactMaskFor(tipo: string): 'telefone' | null {
  const t = (tipo || '').toUpperCase();
  if (t === 'TELEFONE' || t === 'WHATSAPP') return 'telefone';
  return null;
}
