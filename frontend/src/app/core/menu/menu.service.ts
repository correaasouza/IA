import { Injectable } from '@angular/core';

export interface MenuItem {
  id: string;
  label: string;
  route: string;
  icon: string;
  roles?: string[];
  perms?: string[];
}

@Injectable({ providedIn: 'root' })
export class MenuService {
  items: MenuItem[] = [
    { id: 'home', label: 'Home', route: '/home', icon: 'home' },
    { id: 'tenants', label: 'Locatários', route: '/tenants', icon: 'domain', roles: ['MASTER_ADMIN'] },
    { id: 'users', label: 'Usuários', route: '/users', icon: 'group', roles: ['MASTER_ADMIN', 'TENANT_ADMIN'], perms: ['USUARIO_MANAGE'] },
    { id: 'roles', label: 'Papéis', route: '/roles', icon: 'security', roles: ['MASTER_ADMIN', 'TENANT_ADMIN'], perms: ['PAPEL_MANAGE'] },
    { id: 'entities', label: 'Cadastros', route: '/entities', icon: 'assignment', roles: ['MASTER_ADMIN','TENANT_ADMIN','USER'] },
    { id: 'metadata', label: 'Metadados', route: '/metadata', icon: 'view_list', roles: ['MASTER_ADMIN','TENANT_ADMIN'] },
    { id: 'configs', label: 'Configurações', route: '/configs', icon: 'settings', roles: ['MASTER_ADMIN','TENANT_ADMIN','CONFIG_EDITOR'], perms: ['CONFIG_EDITOR'] },
    { id: 'reports', label: 'Relatórios', route: '/reports', icon: 'bar_chart', roles: ['MASTER_ADMIN','TENANT_ADMIN'], perms: ['RELATORIO_VIEW'] },
    { id: 'help', label: 'Ajuda', route: '/help', icon: 'help', roles: ['MASTER_ADMIN','TENANT_ADMIN','USER'] }
  ];
}
