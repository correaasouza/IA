import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class FeatureFlagService {

  isEnabled(key: string, defaultValue = true): boolean {
    const flags = this.readFlags();
    const normalizedKey = (key || '').trim();
    if (!normalizedKey) {
      return defaultValue;
    }
    if (!(normalizedKey in flags)) {
      return defaultValue;
    }
    return !!flags[normalizedKey];
  }

  private readFlags(): Record<string, boolean> {
    try {
      const raw = localStorage.getItem('featureFlags');
      const parsed = raw ? JSON.parse(raw) : {};
      if (!parsed || typeof parsed !== 'object') {
        return {};
      }
      const result: Record<string, boolean> = {};
      for (const [key, value] of Object.entries(parsed)) {
        result[key] = !!value;
      }
      return result;
    } catch {
      return {};
    }
  }
}
