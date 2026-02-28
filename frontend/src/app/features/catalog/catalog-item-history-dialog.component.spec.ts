import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { CatalogItemHistoryDialogComponent } from './catalog-item-history-dialog.component';
import { CatalogPricingService } from './catalog-pricing.service';
import { CatalogStockService } from './catalog-stock.service';

describe('CatalogItemHistoryDialogComponent', () => {
  let fixture: ComponentFixture<CatalogItemHistoryDialogComponent>;
  let component: CatalogItemHistoryDialogComponent;
  let stockService: jasmine.SpyObj<CatalogStockService>;
  let pricingService: jasmine.SpyObj<CatalogPricingService>;

  beforeEach(async () => {
    stockService = jasmine.createSpyObj<CatalogStockService>('CatalogStockService', ['getBalances', 'getLedger']);
    pricingService = jasmine.createSpyObj<CatalogPricingService>('CatalogPricingService', ['getPriceHistory']);

    stockService.getBalances.and.returnValue(of({
      catalogoId: 99,
      agrupadorEmpresaId: 1,
      rows: [],
      consolidado: []
    }));
    stockService.getLedger.and.returnValue(of({ content: [], totalElements: 0 }));
    pricingService.getPriceHistory.and.returnValue(of({ content: [], totalElements: 0 }));

    await TestBed.configureTestingModule({
      imports: [CatalogItemHistoryDialogComponent],
      providers: [
        { provide: CatalogStockService, useValue: stockService },
        { provide: CatalogPricingService, useValue: pricingService },
        { provide: MatDialogRef, useValue: { close: jasmine.createSpy('close') } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            type: 'PRODUCTS',
            itemId: 99,
            itemCodigo: 1234,
            itemNome: 'Item teste'
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogItemHistoryDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should apply ledger filters with movement origin fields', () => {
    component.ledgerFilters.patchValue({
      origemTipo: 'MOVIMENTO_ESTOQUE',
      origemCodigo: 'MV-123',
      usuario: 'joao',
      fromDate: '01/02/2026',
      toDate: '28/02/2026'
    });

    component.applyLedgerFilters();

    const params = stockService.getLedger.calls.mostRecent().args[2]!;
    expect(params.origemTipo).toBe('MOVIMENTO_ESTOQUE');
    expect(params.origemCodigo).toBe('MV-123');
    expect(params.usuario).toBe('joao');
    expect(params.fromDate).toBe('2026-02-01');
    expect(params.toDate).toBe('2026-02-28');
    expect(typeof params.tzOffsetMinutes).toBe('number');
  });

  it('should apply price history filters', () => {
    component.ledgerFilters.patchValue({
      fromDate: '01/02/2026',
      toDate: '28/02/2026'
    });

    component.applyLedgerFilters();

    const params = pricingService.getPriceHistory.calls.mostRecent().args[2]!;
    expect(params.fromDate).toBe('2026-02-01');
    expect(params.toDate).toBe('2026-02-28');
    expect(typeof params.tzOffsetMinutes).toBe('number');
  });
});
