import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AgrupadorEmpresaService } from './agrupador-empresa.service';
import { environment } from '../../../environments/environment';

describe('AgrupadorEmpresaService', () => {
  let service: AgrupadorEmpresaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AgrupadorEmpresaService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AgrupadorEmpresaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should list agrupadores by config scope', () => {
    const payload = [{ id: 1, nome: 'Grupo A', ativo: true, empresas: [] }];

    service.list('FORMULARIO', 10).subscribe(response => {
      expect(response.length).toBe(1);
      expect(response[0]?.nome).toBe('Grupo A');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/configuracoes/FORMULARIO/10/agrupadores-empresa`);
    expect(req.request.method).toBe('GET');
    req.flush(payload);
  });

  it('should create agrupador', () => {
    service.create('COLUNA', 8, 'Grupo B').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/configuracoes/COLUNA/8/agrupadores-empresa`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nome: 'Grupo B' });
    req.flush({ id: 2, nome: 'Grupo B', ativo: true, empresas: [] });
  });

  it('should add empresa to agrupador', () => {
    service.addEmpresa('FORMULARIO', 11, 20, 5).subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/configuracoes/FORMULARIO/11/agrupadores-empresa/20/empresas`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ empresaId: 5 });
    req.flush({ id: 20, nome: 'Grupo', ativo: true, empresas: [{ empresaId: 5, nome: 'Filial' }] });
  });

  it('should remove empresa from agrupador', () => {
    service.removeEmpresa('FORMULARIO', 11, 20, 5).subscribe();

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/configuracoes/FORMULARIO/11/agrupadores-empresa/20/empresas/5`
    );
    expect(req.request.method).toBe('DELETE');
    req.flush({ id: 20, nome: 'Grupo', ativo: true, empresas: [] });
  });
});
