import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { CatalogItemService } from './catalog-item.service';

describe('CatalogItemService', () => {
  let service: CatalogItemService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CatalogItemService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(CatalogItemService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should call context endpoint', () => {
    service.contextoEmpresa('PRODUCTS').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/PRODUCTS/contexto-empresa`);
    expect(req.request.method).toBe('GET');
    req.flush({
      empresaId: 1,
      empresaNome: 'Empresa',
      type: 'PRODUCTS',
      catalogConfigurationId: 9,
      agrupadorId: 2,
      agrupadorNome: 'Grupo A',
      numberingMode: 'AUTOMATICA',
      vinculado: true,
      motivo: null,
      mensagem: null
    });
  });

  it('should list items with query params', () => {
    service.list('SERVICES', { page: 0, size: 20, text: 'oleo', grupoId: 10, ativo: true }).subscribe();

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/catalog/SERVICES/items?page=0&size=20&text=oleo&grupoId=10&ativo=true`
    );
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0 });
  });

  it('should update item', () => {
    service.update('PRODUCTS', 7, { nome: 'Novo', tenantUnitId: 'unit-1', ativo: true }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/PRODUCTS/items/7`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ nome: 'Novo', tenantUnitId: 'unit-1', ativo: true });
    req.flush({
      id: 7,
      type: 'PRODUCTS',
      catalogConfigurationId: 1,
      agrupadorEmpresaId: 1,
      codigo: 10,
      nome: 'Novo',
      descricao: null,
      tenantUnitId: 'unit-1',
      ativo: true
    });
  });
});
