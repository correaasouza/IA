import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { CompanyScopeGuard } from './company-scope.guard';

describe('CompanyScopeGuard', () => {
  let guard: CompanyScopeGuard;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    TestBed.configureTestingModule({
      providers: [
        CompanyScopeGuard,
        { provide: Router, useValue: router }
      ]
    });
    guard = TestBed.inject(CompanyScopeGuard);
    localStorage.clear();
  });

  it('allows when tenant and company are selected', () => {
    localStorage.setItem('tenantId', '2');
    localStorage.setItem('empresaContextId', '10');
    expect(guard.canActivate()).toBeTrue();
  });

  it('blocks when company context is missing', () => {
    localStorage.setItem('tenantId', '2');
    expect(guard.canActivate()).toBeFalse();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/home');
  });
});
