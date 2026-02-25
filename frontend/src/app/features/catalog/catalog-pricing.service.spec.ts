import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { CatalogPricingService } from './catalog-pricing.service';

describe('CatalogPricingService', () => {
  let service: CatalogPricingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CatalogPricingService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(CatalogPricingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load catalog price history with expected query params', () => {
    service.getPriceHistory('PRODUCTS', 90, {
      sourceType: 'SALE_PRICE',
      priceType: 'SALE_BASE',
      priceBookId: 10,
      text: 'joao',
      fromDate: '2026-02-01',
      toDate: '2026-02-29',
      tzOffsetMinutes: 180,
      page: 1,
      size: 25
    }).subscribe();

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/catalog/PRODUCTS/items/90/price/history?sourceType=SALE_PRICE&priceType=SALE_BASE&priceBookId=10&text=joao&fromDate=2026-02-01&toDate=2026-02-29&tzOffsetMinutes=180&page=1&size=25`
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0 });
  });
});
