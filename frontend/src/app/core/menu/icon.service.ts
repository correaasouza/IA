import { Injectable } from '@angular/core';
import { MenuService, MenuItem } from './menu.service';

@Injectable({ providedIn: 'root' })
export class IconService {
  private iconMap: Record<string, string> = {
    home: 'home',
    tenants: 'domain',
    users: 'group',
    roles: 'security',
    entities: 'assignment',
    metadata: 'view_list',
    configs: 'settings',
    reports: 'bar_chart'
  };

  resolveIcon(menuId: string, defaultIcon: string) {
    return this.iconMap[menuId] || defaultIcon;
  }

  setIcon(menuId: string, icon: string) {
    this.iconMap[menuId] = icon;
  }
}
