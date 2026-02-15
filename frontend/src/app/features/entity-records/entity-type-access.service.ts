import { Injectable } from '@angular/core';
import { AccessControlService } from '../../core/access/access-control.service';

export interface EntityTypeAccessRef {
  id: number;
  codigoSeed?: string | null;
}

@Injectable({ providedIn: 'root' })
export class EntityTypeAccessService {
  constructor(private access: AccessControlService) {}

  fallbackRoles(): string[] {
    return ['MASTER', 'ADMIN'];
  }

  controlKeyFor(type: EntityTypeAccessRef): string {
    const seed = (type?.codigoSeed || '').trim().toUpperCase();
    if (seed === 'CLIENTE') return 'menu.entities-clientes';
    if (seed === 'FORNECEDOR') return 'menu.entities-fornecedores';
    if (seed === 'EQUIPE') return 'menu.entities-equipe';
    return `menu.entities.tipo.${type?.id || 0}`;
  }

  canAccessType(type: EntityTypeAccessRef): boolean {
    return this.access.can(this.controlKeyFor(type), this.fallbackRoles());
  }
}

