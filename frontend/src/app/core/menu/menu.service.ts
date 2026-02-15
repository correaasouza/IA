import { Injectable } from '@angular/core';

export interface MenuItem {
  id: string;
  label: string;
  route?: string;
  icon: string;
  roles?: string[];
  perms?: string[];
  children?: MenuItem[];
}

@Injectable({ providedIn: 'root' })
export class MenuService {
  items: MenuItem[] = [
    {
      id: 'group-access',
      label: 'Acessos',
      icon: 'folder',
      children: [
        { id: 'tenants', label: 'Locatários', route: '/tenants', icon: 'domain', roles: ['MASTER'] },
        { id: 'users', label: 'Usuários', route: '/users', icon: 'group', roles: ['MASTER', 'ADMIN'], perms: ['USUARIO_MANAGE'] },
        { id: 'roles', label: 'Papéis', route: '/roles', icon: 'security', roles: ['MASTER'] },
        { id: 'access-controls', label: 'Acessos UI', route: '/access-controls', icon: 'tune', roles: ['MASTER', 'ADMIN'] }
      ]
    },
    {
      id: 'group-config',
      label: 'Configurações',
      icon: 'settings',
      children: [
        { id: 'entity-types', label: 'Tipos de Entidade', route: '/entity-types', icon: 'category', roles: ['MASTER', 'ADMIN'], perms: ['ENTIDADE_EDIT'] }
      ]
    }
  ];
}


