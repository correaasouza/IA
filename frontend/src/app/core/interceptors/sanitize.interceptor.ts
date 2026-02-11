import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpResponse
} from '@angular/common/http';
import { Observable, map } from 'rxjs';

@Injectable()
export class SanitizeInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      map(event => {
        if (!(event instanceof HttpResponse)) {
          return event;
        }
        if (event.body == null) {
          return event;
        }
        const cleaned = sanitizeDeep(event.body);
        return event.clone({ body: cleaned });
      })
    );
  }
}

function sanitizeDeep(value: unknown): unknown {
  if (typeof value === 'string') {
    return sanitizeText(value);
  }
  if (Array.isArray(value)) {
    return value.map(item => sanitizeDeep(item));
  }
  if (value && typeof value === 'object') {
    const record = value as Record<string, unknown>;
    const next: Record<string, unknown> = {};
    Object.keys(record).forEach(key => {
      next[key] = sanitizeDeep(record[key]);
    });
    return next;
  }
  return value;
}

function sanitizeText(value: string): string {
  let cleaned = value;
  if (cleaned.includes('\uFFFD')) {
    const patches: Array<[RegExp, string]> = [
      [/C\uFFFDdigo/gi, 'Código'],
      [/Funcion\uFFFDrio/gi, 'Funcionário'],
      [/Usu\uFFFDrio/gi, 'Usuário'],
      [/Relat\uFFFDrio/gi, 'Relatório'],
      [/Pap\uFFFDis/gi, 'Papéis'],
      [/Locat\uFFFDrio/gi, 'Locatário'],
      [/Formul\uFFFDrio/gi, 'Formulário']
    ];
    patches.forEach(([pattern, replacement]) => {
      cleaned = cleaned.replace(pattern, replacement);
    });
    cleaned = cleaned.replace(/\uFFFD/g, '');
  }
  if (/[ÃÂ]/.test(cleaned)) {
    try {
      const bytes = new Uint8Array([...cleaned].map(ch => ch.charCodeAt(0)));
      const decoded = new TextDecoder('utf-8', { fatal: false }).decode(bytes);
      return decoded;
    } catch {
      return cleaned;
    }
  }
  return cleaned;
}
