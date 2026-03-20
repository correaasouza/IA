import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MasterOnlyGuard } from './master-only.guard';
import { AuthService } from '../auth/auth.service';

describe('MasterOnlyGuard', () => {
  let guard: MasterOnlyGuard;
  let router: jasmine.SpyObj<Router>;
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['getUsername', 'getUserRoles']);
    TestBed.configureTestingModule({
      providers: [
        MasterOnlyGuard,
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: auth }
      ]
    });
    guard = TestBed.inject(MasterOnlyGuard);
    localStorage.clear();
  });

  it('allows global master on tenant 1', () => {
    localStorage.setItem('tenantId', '1');
    auth.getUsername.and.returnValue('master');
    auth.getUserRoles.and.returnValue(['ROLE_MASTER']);
    expect(guard.canActivate()).toBeTrue();
  });

  it('allows global master without tenant context', () => {
    auth.getUsername.and.returnValue('master');
    auth.getUserRoles.and.returnValue([]);
    expect(guard.canActivate()).toBeTrue();
  });

  it('blocks non-master', () => {
    localStorage.setItem('tenantId', '2');
    auth.getUsername.and.returnValue('user1');
    auth.getUserRoles.and.returnValue(['ROLE_USER']);
    expect(guard.canActivate()).toBeFalse();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/home');
  });
});
