import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { MovementOperationService } from './movement-operation.service';

describe('MovementOperationService', () => {
  let service: MovementOperationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MovementOperationService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(MovementOperationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should build movement estoque template', () => {
    service.buildTemplate('MOVIMENTO_ESTOQUE', 3).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/movimentos/MOVIMENTO_ESTOQUE/template`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.empresaId).toBe(3);
    req.flush({
      tipoMovimento: 'MOVIMENTO_ESTOQUE',
      empresaId: 3,
      movimentoConfigId: 12,
      tipoEntidadePadraoId: null,
      tiposEntidadePermitidos: [],
      tiposItensPermitidos: [],
      nome: ''
    });
  });

  it('should list movimento estoque with filters', () => {
    service.listEstoque({ page: 1, size: 10, nome: 'ajuste' }).subscribe();

    const req = httpMock.expectOne((request) =>
      request.url === `${environment.apiBaseUrl}/api/movimentos/MOVIMENTO_ESTOQUE`
      && request.params.get('page') === '1'
      && request.params.get('size') === '10'
      && request.params.get('nome') === 'ajuste');
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 1, size: 10 });
  });

  it('should create movimento estoque', () => {
    service.createEstoque({
      empresaId: 3,
      nome: 'Movimento de ajuste',
      itens: []
    }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/movimentos/MOVIMENTO_ESTOQUE`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.nome).toBe('Movimento de ajuste');
    req.flush({
      id: 1,
      tipoMovimento: 'MOVIMENTO_ESTOQUE',
      empresaId: 3,
      nome: 'Movimento de ajuste',
      movimentoConfigId: 12,
      tipoEntidadePadraoId: null,
      itens: [],
      totalItens: 0,
      totalCobrado: 0,
      version: 0
    });
  });

  it('should search catalog items by tipo item', () => {
    service.searchCatalogItemsByTipoItem(9, 'oleo', 0, 15).subscribe();

    const req = httpMock.expectOne((request) =>
      request.url === `${environment.apiBaseUrl}/api/movimentos/MOVIMENTO_ESTOQUE/catalogo-itens`
      && request.params.get('tipoItemId') === '9'
      && request.params.get('text') === 'oleo'
      && request.params.get('page') === '0'
      && request.params.get('size') === '15');
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 15 });
  });

  it('should search catalog items with new endpoint', () => {
    service.searchCatalogItems({
      movementType: 'MOVIMENTO_ESTOQUE',
      movementConfigId: 12,
      movementItemTypeId: 9,
      q: 'oleo',
      groupId: 4,
      includeDescendants: true,
      ativo: true,
      page: 0,
      size: 30
    }).subscribe();

    const req = httpMock.expectOne((request) =>
      request.url === `${environment.apiBaseUrl}/api/catalog-items/search`
      && request.params.get('movementType') === 'MOVIMENTO_ESTOQUE'
      && request.params.get('movementConfigId') === '12'
      && request.params.get('movementItemTypeId') === '9'
      && request.params.get('q') === 'oleo'
      && request.params.get('groupId') === '4'
      && request.params.get('includeDescendants') === 'true'
      && request.params.get('ativo') === 'true'
      && request.params.get('page') === '0'
      && request.params.get('size') === '30');
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 30 });
  });

  it('should load lazy catalog group children', () => {
    service.loadCatalogGroupChildren({
      movementType: 'MOVIMENTO_ESTOQUE',
      movementConfigId: 12,
      movementItemTypeId: 9,
      parentId: 3
    }).subscribe();

    const req = httpMock.expectOne((request) =>
      request.url === `${environment.apiBaseUrl}/api/catalog-groups/tree`
      && request.params.get('movementType') === 'MOVIMENTO_ESTOQUE'
      && request.params.get('movementConfigId') === '12'
      && request.params.get('movementItemTypeId') === '9'
      && request.params.get('parentId') === '3');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should append movement items in batch', () => {
    service.appendMovementItems(55, {
      items: [{
        movementItemTypeId: 9,
        catalogItemId: 200,
        quantidade: 2,
        valorUnitario: 10
      }]
    }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/movements/55/items`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.items.length).toBe(1);
    req.flush({
      movementId: 55,
      addedCount: 1,
      itemsAdded: [],
      totalItens: 1,
      totalCobrado: 20
    });
  });
});
