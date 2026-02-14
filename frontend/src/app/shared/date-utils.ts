export const DATE_INPUT_FORMAT = 'DD/MM/AAAA';

export function toIsoDate(input: string): string {
  const parts = parseDateInput(input);
  if (!parts) return input;
  return `${String(parts.year).padStart(4, '0')}-${String(parts.month).padStart(2, '0')}-${String(parts.day).padStart(2, '0')}`;
}

export function toDisplayDate(input: string): string {
  const parts = parseDateInput(input);
  if (!parts) return input;
  return `${String(parts.day).padStart(2, '0')}/${String(parts.month).padStart(2, '0')}/${String(parts.year).padStart(4, '0')}`;
}

export function isValidDateInput(input: string): boolean {
  const parts = parseDateInput(input);
  if (!parts) return false;
  return isValidDateParts(parts.year, parts.month, parts.day);
}

function parseDateInput(input: string): { year: number; month: number; day: number } | null {
  if (!input) return null;
  const value = input.trim();

  // Padrão do sistema: DD/MM/AAAA (também aceita '-')
  const std = /^([0-9]{2})[\/-]([0-9]{2})[\/-]([0-9]{4})$/.exec(value);
  if (std) {
    return { year: Number(std[3]), month: Number(std[2]), day: Number(std[1]) };
  }

  // Compatibilidade para ISO vindo da API
  const iso = /^([0-9]{4})[\/-]([0-9]{2})[\/-]([0-9]{2})$/.exec(value);
  if (iso) {
    return { year: Number(iso[1]), month: Number(iso[2]), day: Number(iso[3]) };
  }
  return null;
}

function isValidDateParts(year: number, month: number, day: number): boolean {
  if (year < 1900 || year > 2999) return false;
  if (month < 1 || month > 12) return false;
  if (day < 1 || day > 31) return false;
  const dt = new Date(Date.UTC(year, month - 1, day));
  return dt.getUTCFullYear() === year && dt.getUTCMonth() === month - 1 && dt.getUTCDate() === day;
}

