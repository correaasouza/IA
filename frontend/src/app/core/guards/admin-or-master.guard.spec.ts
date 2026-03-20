import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AdminOrMasterGuard } from './admin-or-master.guard';
import { AuthService } from '../auth/auth.service';

describe('AdminOrMasterGuard', () => {
  let guard: AdminOrMasterGuard;
  let router: jasmine.SpyObj<Router>;
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['getUsername', 'getUserRoles']);
    TestBed.configureTestingModule({
      providers: [
        AdminOrMasterGuard,
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: auth }
      ]
    });
    guard = TestBed.inject(AdminOrMasterGuard);
    localStorage.clear();
  });

  it('allows admin role', () => {
    localStorage.setItem('tenantId', '3');
    localStorage.setItem('tenantRoles:3', JSON.stringify(['ADMIN']));
    auth.getUsername.and.returnValue('admin');
    auth.getUserRoles.and.returnValue(['ROLE_USER']);
    expect(guard.canActivate()).toBeTrue();
  });

  it('blocks user role', () => {
    localStorage.setItem('tenantId', '3');
    auth.getUsername.and.returnValue('user1');
    auth.getUserRoles.and.returnValue(['ROLE_USER']);
    expect(guard.canActivate()).toBeFalse();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/home');
  });
});
