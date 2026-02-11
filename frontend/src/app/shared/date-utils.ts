export function toIsoDate(input: string): string {
  if (!input) return input;
  const m = /^([0-9]{2})[\/-]([0-9]{2})[\/-]([0-9]{4})$/.exec(input.trim());
  if (!m) return input;
  return `${m[3]}-${m[2]}-${m[1]}`;
}
