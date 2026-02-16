import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class IconService {
  private iconMap: Record<string, string> = {
    home: 'home',
    'group-home': 'folder',
    'group-access': 'folder',
    'group-cadastros': 'folder',
    'group-config': 'folder',
    'group-settings': 'folder',
    'group-reports': 'folder',
    'group-help': 'folder',
    tenants: 'domain',
    users: 'group',
    roles: 'security',
    entities: 'assignment',
    'entities-clientes': 'badge',
    'entities-fornecedores': 'local_shipping',
    'entities-equipe': 'groups',
    'entities-outras': 'view_list',
    metadata: 'view_list',
    'entities-config': 'fact_check',
    reports: 'bar_chart',
    'catalog-config': 'inventory_2',
    'catalog-products': 'inventory_2',
    'catalog-services': 'design_services'
  };

  resolveIcon(menuId: string, defaultIcon: string) {
    return this.iconMap[menuId] || defaultIcon;
  }

  setIcon(menuId: string, icon: string) {
    this.iconMap[menuId] = icon;
  }
}
