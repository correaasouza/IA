import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
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
export class CatalogConfigurationPageComponent implements OnInit, OnDestroy {
  activeTab: CatalogConfigurationType = 'PRODUCTS';
  activeInnerTab: 'GROUP_CONFIG' | 'PRICE_BOOKS' | 'PRICE_VARIANTS' | 'STOCK_ADJUSTMENT_TYPES' = 'GROUP_CONFIG';
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
    this.route.queryParamMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const subTab = params.get('subTab');
        if (subTab === 'PRICE_BOOKS' || subTab === 'PRICE_VARIANTS' || subTab === 'STOCK_ADJUSTMENT_TYPES') {
          this.activeInnerTab = subTab;
          return;
        }
        this.activeInnerTab = 'GROUP_CONFIG';
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setTab(tab: CatalogConfigurationType): void {
    this.activeTab = tab;
  }

  isActive(tab: CatalogConfigurationType): boolean {
    return this.activeTab === tab;
  }
}
