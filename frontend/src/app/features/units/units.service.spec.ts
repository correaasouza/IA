import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { UnitsService } from './units.service';
import { environment } from '../../../environments/environment';

describe('UnitsService', () => {
  let service: UnitsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UnitsService]
    });
    service = TestBed.inject(UnitsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should call official units list endpoint with filters', () => {
    service.listOfficial({ ativo: true, text: 'kg' }).subscribe();

    const req = httpMock.expectOne(request =>
      request.url === `${environment.apiBaseUrl}/api/global/official-units`
      && request.params.get('ativo') === 'true'
      && request.params.get('text') === 'kg'
    );
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should call tenant reconcile endpoint', () => {
    service.reconcileTenantUnits().subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/tenant/units/reconcile`);
    expect(req.request.method).toBe('POST');
    req.flush({ tenantId: 1, createdMirrors: 0 });
  });

  it('should call tenant unit conversions list endpoint', () => {
    service.listTenantUnitConversions().subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/tenant/unit-conversions`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});

