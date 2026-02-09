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
import { MetadataComponent } from './features/metadata/metadata.component';
import { ConfigsComponent } from './features/configs/configs.component';
import { EntitiesListComponent } from './features/entities/entities-list.component';
import { EntityFormComponent } from './features/entities/entity-form.component';
import { EntitiesConfigComponent } from './features/entities/entities-config.component';
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
  { path: 'entities/config', component: EntitiesConfigComponent, canActivate: [AuthGuard] },
  { path: 'metadata', component: MetadataComponent, canActivate: [AuthGuard] },
  { path: 'configs', component: ConfigsComponent, canActivate: [AuthGuard] },
  { path: 'reports', component: ReportsComponent, canActivate: [AuthGuard] },
  { path: 'help', component: HelpComponent, canActivate: [AuthGuard] },
  { path: 'blocked', component: BlockedComponent }
];
