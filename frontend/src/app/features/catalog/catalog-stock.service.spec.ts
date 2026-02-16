import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { CatalogStockService } from './catalog-stock.service';

describe('CatalogStockService', () => {
  let service: CatalogStockService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CatalogStockService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(CatalogStockService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load balances by catalog item', () => {
    service.getBalances('PRODUCTS', 10).subscribe();

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/catalog/PRODUCTS/items/10/stock/balances`
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      catalogoId: 10,
      agrupadorEmpresaId: 4,
      rows: [],
      consolidado: []
    });
  });

  it('should load ledger with pagination and filters', () => {
    service.getLedger('SERVICES', 22, {
      page: 1,
      size: 20,
      metricType: 'QUANTIDADE',
      origemTipo: 'MUDANCA_GRUPO'
    }).subscribe();

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/catalog/SERVICES/items/22/stock/ledger?page=1&size=20&metricType=QUANTIDADE&origemTipo=MUDANCA_GRUPO`
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      content: [],
      totalElements: 0
    });
  });
});
