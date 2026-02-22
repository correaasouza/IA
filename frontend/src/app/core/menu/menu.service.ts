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
      id: 'group-stock',
      label: 'Estoque',
      icon: 'inventory',
      children: [
        {
          id: 'catalog-products',
          label: 'Produtos',
          route: '/catalog/products',
          accessKey: 'menu.catalog-products',
          icon: 'inventory_2',
          roles: ['MASTER', 'ADMIN'],
          perms: ['CONFIG_EDITOR']
        },
        {
          id: 'catalog-services',
          label: 'Servicos',
          route: '/catalog/services',
          accessKey: 'menu.catalog-services',
          icon: 'design_services',
          roles: ['MASTER', 'ADMIN'],
          perms: ['CONFIG_EDITOR']
        },
        {
          id: 'stock-movements',
          label: 'Movimento de Estoque',
          route: '/movimentos/estoque',
          accessKey: 'menu.movement.action.movimento_estoque',
          icon: 'sync_alt',
          roles: ['MASTER', 'ADMIN'],
          perms: ['MOVIMENTO_ESTOQUE_OPERAR']
        },
        {
          id: 'catalog-sale-prices',
          label: 'Preços Venda',
          route: '/catalog/pricing/sale-prices',
          accessKey: 'menu.catalog-sale-prices',
          icon: 'table_chart',
          roles: ['MASTER', 'ADMIN'],
          perms: ['CATALOG_PRICES_VIEW']
        }
      ]
    },
    {
      id: 'group-config',
      label: 'Configurações',
      icon: 'settings',
      children: [
        {
          id: 'catalog-config',
          label: 'Catálogo',
          route: '/catalog/configuration',
          accessKey: 'menu.catalog-config',
          icon: 'inventory_2',
          roles: ['MASTER', 'ADMIN'],
          perms: ['CONFIG_EDITOR']
        },
        {
          id: 'movement-configs',
          label: 'Movimentos',
          route: '/configs/movimentos',
          accessKey: 'menu.movement-configs',
          icon: 'sync_alt',
          roles: ['MASTER', 'ADMIN'],
          perms: ['CONFIG_EDITOR']
        },
        {
          id: 'tenant-units',
          label: 'Unidades Medida',
          route: '/tenant-units',
          accessKey: 'menu.tenant-units',
          icon: 'straighten',
          roles: ['MASTER', 'ADMIN'],
          perms: ['CONFIG_EDITOR']
        },
        {
          id: 'tenant-unit-conversions',
          label: 'Conversoes Unid.',
          route: '/tenant-unit-conversions',
          accessKey: 'menu.tenant-unit-conversions',
          icon: 'swap_horiz',
          roles: ['MASTER', 'ADMIN'],
          perms: ['CONFIG_EDITOR']
        },        
        { id: 'entity-types', label: 'Tipos Ent.', route: '/entity-types', icon: 'category', roles: ['MASTER', 'ADMIN'], perms: ['ENTIDADE_EDIT'] }
      ]
    },
    {
      id: 'group-globals',
      label: 'Globais',
      icon: 'public',
      children: [
        {
          id: 'global-official-units',
          label: 'Unidades Oficiais',
          route: '/global-settings/official-units',
          accessKey: 'menu.global-official-units',
          icon: 'public',
          roles: ['MASTER']
        }
      ]
    }
  ];
}


