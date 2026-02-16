import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { CatalogConfigurationService } from './catalog-configuration.service';

describe('CatalogConfigurationService', () => {
  let service: CatalogConfigurationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CatalogConfigurationService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(CatalogConfigurationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should get products catalog configuration', () => {
    service.get('PRODUCTS').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/configuration/PRODUCTS`);
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 1,
      type: 'PRODUCTS',
      numberingMode: 'AUTOMATICA',
      active: true,
      version: 0,
      createdAt: '2026-02-16T01:00:00Z',
      updatedAt: '2026-02-16T01:00:00Z'
    });
  });

  it('should update services catalog configuration numbering mode', () => {
    service.update('SERVICES', { numberingMode: 'MANUAL' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/configuration/SERVICES`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ numberingMode: 'MANUAL' });
    req.flush({
      id: 2,
      type: 'SERVICES',
      numberingMode: 'MANUAL',
      active: true,
      version: 1,
      createdAt: '2026-02-16T01:00:00Z',
      updatedAt: '2026-02-16T01:02:00Z'
    });
  });

  it('should list group configurations by catalog type', () => {
    service.listByGroup('PRODUCTS').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/configuration/PRODUCTS/group-config`);
    expect(req.request.method).toBe('GET');
    req.flush([{ agrupadorId: 10, agrupadorNome: 'Grupo A', numberingMode: 'AUTOMATICA', active: true }]);
  });

  it('should update a group configuration numbering mode', () => {
    service.updateByGroup('SERVICES', 12, { numberingMode: 'MANUAL' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/configuration/SERVICES/group-config/12`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ numberingMode: 'MANUAL' });
    req.flush({ agrupadorId: 12, agrupadorNome: 'Grupo B', numberingMode: 'MANUAL', active: true });
  });
});
