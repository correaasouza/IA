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

  it('should list stock types by group', () => {
    service.listStockTypesByGroup('PRODUCTS', 10).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/configuration/PRODUCTS/group-config/10/stock-types`);
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 1, codigo: 'GERAL', nome: 'Estoque Geral', ordem: 1, active: true, version: 0 }]);
  });

  it('should create stock type by group', () => {
    service.createStockTypeByGroup('SERVICES', 12, { codigo: 'A', nome: 'Estoque A', ordem: 2, active: true }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/configuration/SERVICES/group-config/12/stock-types`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ codigo: 'A', nome: 'Estoque A', ordem: 2, active: true });
    req.flush({ id: 2, codigo: 'A', nome: 'Estoque A', ordem: 2, active: true, version: 0 });
  });

  it('should update stock type by group', () => {
    service.updateStockTypeByGroup('PRODUCTS', 10, 1, { codigo: 'GERAL', nome: 'Estoque Geral', ordem: 1, active: false }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/configuration/PRODUCTS/group-config/10/stock-types/1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ codigo: 'GERAL', nome: 'Estoque Geral', ordem: 1, active: false });
    req.flush({ id: 1, codigo: 'GERAL', nome: 'Estoque Geral', ordem: 1, active: false, version: 1 });
  });

  it('should list stock adjustments by catalog type', () => {
    service.listStockAdjustmentsByType('PRODUCTS').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/configuration/PRODUCTS/stock-adjustments`);
    expect(req.request.method).toBe('GET');
    req.flush([{
      id: 1,
      codigo: 'PADRAO',
      nome: 'Ajuste Padrao',
      tipo: 'ENTRADA',
      ordem: 1,
      active: true,
      version: 0,
      estoqueOrigemAgrupadorId: null,
      estoqueOrigemTipoId: null,
      estoqueOrigemFilialId: null,
      estoqueDestinoAgrupadorId: 10,
      estoqueDestinoTipoId: 3,
      estoqueDestinoFilialId: 2
    }]);
  });

  it('should list stock adjustment scope options by catalog type', () => {
    service.listStockAdjustmentScopeOptionsByType('PRODUCTS').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/configuration/PRODUCTS/stock-adjustments/options`);
    expect(req.request.method).toBe('GET');
    req.flush([{
      agrupadorId: 10,
      agrupadorNome: 'Grupo 1',
      estoqueTipoId: 3,
      estoqueTipoCodigo: 'GERAL',
      estoqueTipoNome: 'Estoque Geral',
      filialId: 2,
      filialNome: 'Matriz',
      label: 'Grupo 1 | GERAL - Estoque Geral | Matriz'
    }]);
  });

  it('should create stock adjustment by catalog type', () => {
    service.createStockAdjustmentByType('SERVICES', {
      nome: 'Ajuste Inicial',
      tipo: 'SAIDA',
      ordem: 2,
      active: true,
      estoqueOrigemAgrupadorId: 12,
      estoqueOrigemTipoId: 4,
      estoqueOrigemFilialId: 5,
      estoqueDestinoAgrupadorId: null,
      estoqueDestinoTipoId: null,
      estoqueDestinoFilialId: null
    }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/configuration/SERVICES/stock-adjustments`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      nome: 'Ajuste Inicial',
      tipo: 'SAIDA',
      ordem: 2,
      active: true,
      estoqueOrigemAgrupadorId: 12,
      estoqueOrigemTipoId: 4,
      estoqueOrigemFilialId: 5,
      estoqueDestinoAgrupadorId: null,
      estoqueDestinoTipoId: null,
      estoqueDestinoFilialId: null
    });
    req.flush({
      id: 2,
      codigo: 'AJ01',
      nome: 'Ajuste Inicial',
      tipo: 'SAIDA',
      ordem: 2,
      active: true,
      version: 0,
      estoqueOrigemAgrupadorId: 12,
      estoqueOrigemTipoId: 4,
      estoqueOrigemFilialId: 5,
      estoqueDestinoAgrupadorId: null,
      estoqueDestinoTipoId: null,
      estoqueDestinoFilialId: null
    });
  });

  it('should update stock adjustment by catalog type', () => {
    service.updateStockAdjustmentByType('PRODUCTS', 8, {
      nome: 'Ajuste Final',
      tipo: 'TRANSFERENCIA',
      ordem: 3,
      active: false,
      estoqueOrigemAgrupadorId: 10,
      estoqueOrigemTipoId: 3,
      estoqueOrigemFilialId: 2,
      estoqueDestinoAgrupadorId: 11,
      estoqueDestinoTipoId: 6,
      estoqueDestinoFilialId: 7
    }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/configuration/PRODUCTS/stock-adjustments/8`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      nome: 'Ajuste Final',
      tipo: 'TRANSFERENCIA',
      ordem: 3,
      active: false,
      estoqueOrigemAgrupadorId: 10,
      estoqueOrigemTipoId: 3,
      estoqueOrigemFilialId: 2,
      estoqueDestinoAgrupadorId: 11,
      estoqueDestinoTipoId: 6,
      estoqueDestinoFilialId: 7
    });
    req.flush({
      id: 8,
      codigo: 'AJ02',
      nome: 'Ajuste Final',
      tipo: 'TRANSFERENCIA',
      ordem: 3,
      active: false,
      version: 1,
      estoqueOrigemAgrupadorId: 10,
      estoqueOrigemTipoId: 3,
      estoqueOrigemFilialId: 2,
      estoqueDestinoAgrupadorId: 11,
      estoqueDestinoTipoId: 6,
      estoqueDestinoFilialId: 7
    });
  });

  it('should delete stock adjustment by catalog type', () => {
    service.deleteStockAdjustmentByType('SERVICES', 9).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/catalog/configuration/SERVICES/stock-adjustments/9`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
