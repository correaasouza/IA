import { Injectable } from '@angular/core';

export interface MenuItem {
  id: string;
  label: string;
  route?: string;
  queryParams?: Record<string, string | number | boolean>;
  accessKey?: string;
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
      id: 'group-cadastros',
      label: 'Cadastros',
      icon: 'folder',
      children: [
        {
          id: 'entities-clientes',
          label: 'Clientes',
          route: '/entities',
          queryParams: { tipoSeed: 'CLIENTE' },
          accessKey: 'menu.entities-clientes',
          icon: 'badge',
          roles: ['MASTER', 'ADMIN'],
          perms: ['ENTIDADE_EDIT']
        },
        {
          id: 'entities-fornecedores',
          label: 'Fornecedores',
          route: '/entities',
          queryParams: { tipoSeed: 'FORNECEDOR' },
          accessKey: 'menu.entities-fornecedores',
          icon: 'local_shipping',
          roles: ['MASTER', 'ADMIN'],
          perms: ['ENTIDADE_EDIT']
        },
        {
          id: 'entities-equipe',
          label: 'Equipe',
          route: '/entities',
          queryParams: { tipoSeed: 'EQUIPE' },
          accessKey: 'menu.entities-equipe',
          icon: 'groups',
          roles: ['MASTER', 'ADMIN'],
          perms: ['ENTIDADE_EDIT']
        }
      ]
    },
    {
      id: 'group-config',
      label: 'Configurações',
      icon: 'settings',
      children: [
        { id: 'entity-types', label: 'Tipos Ent.', route: '/entity-types', icon: 'category', roles: ['MASTER', 'ADMIN'], perms: ['ENTIDADE_EDIT'] }
      ]
    }
  ];
}


