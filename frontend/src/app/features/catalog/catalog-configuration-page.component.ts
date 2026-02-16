import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { CatalogConfigurationFormComponent } from './catalog-configuration-form.component';
import { CatalogConfigurationType } from './catalog-configuration.service';

@Component({
  selector: 'app-catalog-configuration-page',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    CatalogConfigurationFormComponent
  ],
  templateUrl: './catalog-configuration-page.component.html',
  styleUrls: ['./catalog-configuration-page.component.css']
})
export class CatalogConfigurationPageComponent {
  activeTab: CatalogConfigurationType = 'PRODUCTS';

  setTab(tab: CatalogConfigurationType): void {
    this.activeTab = tab;
  }

  isActive(tab: CatalogConfigurationType): boolean {
    return this.activeTab === tab;
  }
}
