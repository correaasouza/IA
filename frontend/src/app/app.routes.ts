import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/home.component';
import { HomeDashboardComponent } from './features/home/home-dashboard.component';
import { AuthGuard } from './core/guards/auth.guard';
import { BlockedComponent } from './features/blocked/blocked.component';
import { UsersListComponent } from './features/users/users-list.component';
import { UserFormComponent } from './features/users/user-form.component';
import { RolesComponent } from './features/roles/roles.component';
import { TenantsListComponent } from './features/tenants/tenants-list.component';
import { TenantFormComponent } from './features/tenants/tenant-form.component';
import { MetadataListComponent } from './features/metadata/metadata-list.component';
import { MetadataFormComponent } from './features/metadata/metadata-form.component';
import { EntitiesListComponent } from './features/entities/entities-list.component';
import { EntityFormComponent } from './features/entities/entity-form.component';
import { ReportsComponent } from './features/reports/reports.component';
import { HelpComponent } from './features/help/help.component';

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
  { path: 'entities', component: EntitiesListComponent, canActivate: [AuthGuard] },
  { path: 'entities/new', component: EntityFormComponent, canActivate: [AuthGuard] },
  { path: 'entities/:id', component: EntityFormComponent, canActivate: [AuthGuard] },
  { path: 'entities/:id/edit', component: EntityFormComponent, canActivate: [AuthGuard] },
  { path: 'entities/config', redirectTo: 'metadata', pathMatch: 'full' },
  { path: 'metadata', component: MetadataListComponent, canActivate: [AuthGuard] },
  { path: 'metadata/new', component: MetadataFormComponent, canActivate: [AuthGuard] },
  { path: 'metadata/:id/edit', component: MetadataFormComponent, canActivate: [AuthGuard] },
  { path: 'configs', redirectTo: 'metadata', pathMatch: 'full' },
  { path: 'reports', component: ReportsComponent, canActivate: [AuthGuard] },
  { path: 'help', component: HelpComponent, canActivate: [AuthGuard] },
  { path: 'blocked', component: BlockedComponent }
];
