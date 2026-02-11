import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class IconService {
  private iconMap: Record<string, string> = {
    home: 'home',
    'group-home': 'folder',
    'group-access': 'folder',
    'group-cadastros': 'folder',
    'group-settings': 'folder',
    'group-reports': 'folder',
    'group-help': 'folder',
    tenants: 'domain',
    users: 'group',
    roles: 'security',
    entities: 'assignment',
    metadata: 'view_list',
    'entities-config': 'fact_check',
    reports: 'bar_chart'
  };

  resolveIcon(menuId: string, defaultIcon: string) {
    return this.iconMap[menuId] || defaultIcon;
  }

  setIcon(menuId: string, icon: string) {
    this.iconMap[menuId] = icon;
  }
}
