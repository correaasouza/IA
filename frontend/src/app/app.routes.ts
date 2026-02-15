import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home.component';
import { HomeDashboardComponent } from './features/home/home-dashboard.component';
import { AuthGuard } from './core/guards/auth.guard';
import { BlockedComponent } from './features/blocked/blocked.component';
import { UsersListComponent } from './features/users/users-list.component';
import { UserFormComponent } from './features/users/user-form.component';
import { RolesComponent } from './features/roles/roles.component';
import { RoleFormComponent } from './features/roles/role-form.component';
import { TenantsListComponent } from './features/tenants/tenants-list.component';
import { TenantFormComponent } from './features/tenants/tenant-form.component';
import { HelpComponent } from './features/help/help.component';
import { CompaniesListComponent } from './features/companies/companies-list.component';
import { CompanyFormComponent } from './features/companies/company-form.component';
import { AccessControlsComponent } from './features/access-controls/access-controls.component';
import { AccessControlFormComponent } from './features/access-controls/access-control-form.component';
import { ConfigsComponent } from './features/configs/configs.component';
import { EntityTypesListComponent } from './features/entity-types/entity-types-list.component';
import { EntityTypeFormComponent } from './features/entity-types/entity-type-form.component';

export const routes: Routes = [
  { path: '', component: HomeComponent, canActivate: [AuthGuard] },
  { path: 'home', component: HomeDashboardComponent, canActivate: [AuthGuard] },
  { path: 'tenants', component: TenantsListComponent, canActivate: [AuthGuard] },
  { path: 'tenants/new', component: TenantFormComponent, canActivate: [AuthGuard] },
  { path: 'tenants/:id', component: TenantFormComponent, canActivate: [AuthGuard] },
  { path: 'tenants/:id/edit', component: TenantFormComponent, canActivate: [AuthGuard] },
  { path: 'users', component: UsersListComponent, canActivate: [AuthGuard] },
  { path: 'users/new', component: UserFormComponent, canActivate: [AuthGuard] },
  { path: 'users/:id', component: UserFormComponent, canActivate: [AuthGuard] },
  { path: 'users/:id/edit', component: UserFormComponent, canActivate: [AuthGuard] },
  { path: 'roles', component: RolesComponent, canActivate: [AuthGuard] },
  { path: 'roles/new', component: RoleFormComponent, canActivate: [AuthGuard] },
  { path: 'roles/:id', component: RoleFormComponent, canActivate: [AuthGuard] },
  { path: 'roles/:id/edit', component: RoleFormComponent, canActivate: [AuthGuard] },
  { path: 'companies', component: CompaniesListComponent, canActivate: [AuthGuard] },
  { path: 'companies/new', component: CompanyFormComponent, canActivate: [AuthGuard] },
  { path: 'companies/:id', component: CompanyFormComponent, canActivate: [AuthGuard] },
  { path: 'companies/:id/edit', component: CompanyFormComponent, canActivate: [AuthGuard] },
  { path: 'access-controls', component: AccessControlsComponent, canActivate: [AuthGuard] },
  { path: 'access-controls/new', component: AccessControlFormComponent, canActivate: [AuthGuard] },
  { path: 'access-controls/:key', component: AccessControlFormComponent, canActivate: [AuthGuard] },
  { path: 'access-controls/:key/edit', component: AccessControlFormComponent, canActivate: [AuthGuard] },
  { path: 'configs', component: ConfigsComponent, canActivate: [AuthGuard] },
  { path: 'entity-types', component: EntityTypesListComponent, canActivate: [AuthGuard] },
  { path: 'entity-types/new', component: EntityTypeFormComponent, canActivate: [AuthGuard] },
  { path: 'entity-types/:id', component: EntityTypeFormComponent, canActivate: [AuthGuard] },
  { path: 'entity-types/:id/edit', component: EntityTypeFormComponent, canActivate: [AuthGuard] },
  { path: 'help', component: HelpComponent, canActivate: [AuthGuard] },
  { path: 'blocked', component: BlockedComponent }
];
