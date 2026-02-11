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
        { id: 'tenants', label: 'Locatários', route: '/tenants', icon: 'domain', roles: ['MASTER_ADMIN'] },
        { id: 'users', label: 'Usuários', route: '/users', icon: 'group', roles: ['MASTER_ADMIN', 'TENANT_ADMIN'], perms: ['USUARIO_MANAGE'] },
        { id: 'roles', label: 'Papéis', route: '/roles', icon: 'security', roles: ['MASTER_ADMIN', 'TENANT_ADMIN'], perms: ['PAPEL_MANAGE'] }
      ]
    },
    {
      id: 'group-cadastros',
      label: 'Entidades',
      icon: 'folder',
      children: [
        { id: 'entities', label: 'Cadastros', route: '/entities', icon: 'assignment', roles: ['MASTER_ADMIN','TENANT_ADMIN','USER'] }
      ]
    },
    {
      id: 'group-settings',
      label: 'Configurações',
      icon: 'folder',
      children: [
        { id: 'metadata', label: 'Tipos de Entidades', route: '/metadata', icon: 'view_list', roles: ['MASTER_ADMIN','TENANT_ADMIN'] }
      ]
    },
    {
      id: 'group-reports',
      label: 'Relatórios',
      icon: 'folder',
      children: [
        { id: 'reports', label: 'Relatórios', route: '/reports', icon: 'bar_chart', roles: ['MASTER_ADMIN','TENANT_ADMIN'], perms: ['RELATORIO_VIEW'] }
      ]
    },
  ];
}
