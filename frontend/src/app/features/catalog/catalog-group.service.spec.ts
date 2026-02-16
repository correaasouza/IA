import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { CatalogGroupService } from './catalog-group.service';

describe('CatalogGroupService', () => {
  let service: CatalogGroupService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CatalogGroupService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(CatalogGroupService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load groups tree by type', () => {
    service.tree('PRODUCTS').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/PRODUCTS/groups/tree`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should create group', () => {
    service.create('SERVICES', { nome: 'Raiz', parentId: null }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/SERVICES/groups`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nome: 'Raiz', parentId: null });
    req.flush({ id: 1, nome: 'Raiz', nivel: 0, ordem: 1, path: '00000001', ativo: true, children: [] });
  });

  it('should delete group', () => {
    service.delete('PRODUCTS', 3).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/PRODUCTS/groups/3`);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });
});
