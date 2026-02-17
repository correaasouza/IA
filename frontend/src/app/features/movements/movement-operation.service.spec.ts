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
      nome: '',
      dataMovimento: '2026-02-17'
    });
  });

  it('should list movimento estoque with filters', () => {
    service.listEstoque({ page: 1, size: 10, nome: 'ajuste', dataInicio: '2025-12-01', dataFim: '2025-12-31' }).subscribe();

    const req = httpMock.expectOne((request) =>
      request.url === `${environment.apiBaseUrl}/api/movimentos/MOVIMENTO_ESTOQUE`
      && request.params.get('page') === '1'
      && request.params.get('size') === '10'
      && request.params.get('nome') === 'ajuste'
      && request.params.get('dataInicio') === '2025-12-01'
      && request.params.get('dataFim') === '2025-12-31');
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 1, size: 10 });
  });

  it('should create movimento estoque', () => {
    service.createEstoque({
      empresaId: 3,
      nome: 'Movimento de ajuste 31/12/2025',
      dataMovimento: '2025-12-31'
    }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/movimentos/MOVIMENTO_ESTOQUE`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.nome).toBe('Movimento de ajuste 31/12/2025');
    req.flush({
      id: 1,
      tipoMovimento: 'MOVIMENTO_ESTOQUE',
      empresaId: 3,
      nome: 'Movimento de ajuste 31/12/2025',
      dataMovimento: '2025-12-31',
      movimentoConfigId: 12,
      tipoEntidadePadraoId: null,
      version: 0
    });
  });
});
