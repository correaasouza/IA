import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../environments/environment';
import { MovementConfigService } from './movement-config.service';

describe('MovementConfigService', () => {
  let service: MovementConfigService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MovementConfigService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(MovementConfigService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should list movement types', () => {
    service.listTipos().subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/movimentos/configuracoes/tipos`);
    expect(req.request.method).toBe('GET');
    req.flush([{ codigo: 'ORDEM_COMPRA', descricao: 'Ordem de Compra' }]);
  });

  it('should list movement configs by type', () => {
    service.listByTipo('ORDEM_COMPRA', 0, 50).subscribe();

    const req = httpMock.expectOne((request) =>
      request.url === `${environment.apiBaseUrl}/api/movimentos/configuracoes`
      && request.params.get('tipo') === 'ORDEM_COMPRA'
      && request.params.get('page') === '0'
      && request.params.get('size') === '50');
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 50 });
  });

  it('should create movement config', () => {
    service.create({
      tipoMovimento: 'ORDEM_COMPRA',
      nome: 'OC principal',
      ativo: true,
      empresaIds: [1],
      tiposEntidadePermitidos: [10],
      tipoEntidadePadraoId: 10
    }).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/movimentos/configuracoes`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.nome).toBe('OC principal');
    req.flush({
      id: 1,
      tipoMovimento: 'ORDEM_COMPRA',
      nome: 'OC principal',
      contextoKey: null,
      ativo: true,
      version: 0,
      empresaIds: [1],
      tiposEntidadePermitidos: [10],
      tipoEntidadePadraoId: 10,
      createdAt: '2026-02-17T01:00:00Z',
      updatedAt: '2026-02-17T01:00:00Z'
    });
  });

  it('should resolve effective movement config', () => {
    service.resolve('ORDEM_COMPRA', 1, 'REVENDA').subscribe();

    const req = httpMock.expectOne((request) =>
      request.url === `${environment.apiBaseUrl}/api/movimentos/configuracoes/resolver`
      && request.params.get('tipo') === 'ORDEM_COMPRA'
      && request.params.get('empresaId') === '1'
      && request.params.get('contextoKey') === 'REVENDA');
    expect(req.request.method).toBe('GET');
    req.flush({
      configuracaoId: 77,
      tipoMovimento: 'ORDEM_COMPRA',
      empresaId: 1,
      contextoKey: 'REVENDA',
      tipoEntidadePadraoId: 10,
      tiposEntidadePermitidos: [10, 11]
    });
  });

  it('should list coverage warnings', () => {
    service.listCoverageWarnings().subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/movimentos/configuracoes/warnings`);
    expect(req.request.method).toBe('GET');
    req.flush([{
      empresaId: 1,
      empresaNome: 'Empresa A',
      tipoMovimento: 'ORDEM_COMPRA',
      mensagem: 'Sem configuracao ativa de fallback para empresa e tipo.'
    }]);
  });

  it('should list menu movement buttons by empresa', () => {
    service.listMenuByEmpresa(22).subscribe();

    const req = httpMock.expectOne((request) =>
      request.url === `${environment.apiBaseUrl}/api/movimentos/configuracoes/menu`
      && request.params.get('empresaId') === '22');
    expect(req.request.method).toBe('GET');
    req.flush([{ codigo: 'PEDIDO_VENDA', descricao: 'Pedido de Venda' }]);
  });
});
