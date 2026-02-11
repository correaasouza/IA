import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';

import { AtalhoService } from '../../core/atalhos/atalho.service';
import { MenuService, MenuItem } from '../../core/menu/menu.service';

@Component({
  selector: 'app-home-dashboard',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, RouterLink],
  templateUrl: './home-dashboard.component.html',
  styleUrls: ['./home-dashboard.component.css']
})
export class HomeDashboardComponent implements OnInit {
  shortcuts: MenuItem[] = [];

  constructor(
    private atalhoService: AtalhoService,
    private menuService: MenuService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const tenantId = localStorage.getItem('tenantId');
    if (!tenantId) {
      this.router.navigateByUrl('/');
      return;
    }
    this.loadShortcuts();
  }

  loadShortcuts() {
    this.atalhoService.list().subscribe({
      next: data => {
        const atalhos = data || [];
        const allowed = this.menuService.items
          .flatMap(item => item.children && item.children.length > 0 ? item.children : [item])
          .filter(item => !!item.route);
        this.shortcuts = atalhos
          .sort((a, b) => a.ordem - b.ordem)
          .map(a => allowed.find(m => m.id === a.menuId))
          .filter(Boolean) as MenuItem[];
      },
      error: () => {
        this.shortcuts = [];
      }
    });
  }
}
