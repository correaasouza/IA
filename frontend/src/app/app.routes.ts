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
import { EntityRecordsListComponent } from './features/entity-records/entity-records-list.component';
import { EntityRecordFormComponent } from './features/entity-records/entity-record-form.component';
import { EntityGroupsPageComponent } from './features/entity-records/entity-groups-page.component';
import { CatalogConfigurationPageComponent } from './features/catalog/catalog-configuration-page.component';
import { CatalogItemsListComponent } from './features/catalog/catalog-items-list.component';
import { CatalogItemFormComponent } from './features/catalog/catalog-item-form.component';
import { CatalogGroupsPageComponent } from './features/catalog/catalog-groups-page.component';
import { MovementConfigsPageComponent } from './features/movement-configs/movement-configs-page.component';
import { MovimentoEstoqueListComponent } from './features/movements/movimento-estoque-list.component';
import { MovimentoEstoqueFormComponent } from './features/movements/movimento-estoque-form.component';
import { OfficialUnitsListComponent } from './features/units/official-units-list.component';
import { OfficialUnitFormComponent } from './features/units/official-unit-form.component';
import { TenantUnitsListComponent } from './features/units/tenant-units-list.component';
import { TenantUnitFormComponent } from './features/units/tenant-unit-form.component';
import { TenantUnitConversionsListComponent } from './features/units/tenant-unit-conversions-list.component';
import { TenantUnitConversionFormComponent } from './features/units/tenant-unit-conversion-form.component';

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
  { path: 'configs/movimentos', component: MovementConfigsPageComponent, canActivate: [AuthGuard] },
  { path: 'global-settings/official-units', component: OfficialUnitsListComponent, canActivate: [AuthGuard] },
  { path: 'global-settings/official-units/new', component: OfficialUnitFormComponent, canActivate: [AuthGuard] },
  { path: 'global-settings/official-units/:id', component: OfficialUnitFormComponent, canActivate: [AuthGuard] },
  { path: 'global-settings/official-units/:id/edit', component: OfficialUnitFormComponent, canActivate: [AuthGuard] },
  { path: 'tenant-units', component: TenantUnitsListComponent, canActivate: [AuthGuard] },
  { path: 'tenant-units/new', component: TenantUnitFormComponent, canActivate: [AuthGuard] },
  { path: 'tenant-units/:id', component: TenantUnitFormComponent, canActivate: [AuthGuard] },
  { path: 'tenant-units/:id/edit', component: TenantUnitFormComponent, canActivate: [AuthGuard] },
  { path: 'tenant-unit-conversions', component: TenantUnitConversionsListComponent, canActivate: [AuthGuard] },
  { path: 'tenant-unit-conversions/new', component: TenantUnitConversionFormComponent, canActivate: [AuthGuard] },
  { path: 'tenant-unit-conversions/:id', component: TenantUnitConversionFormComponent, canActivate: [AuthGuard] },
  { path: 'tenant-unit-conversions/:id/edit', component: TenantUnitConversionFormComponent, canActivate: [AuthGuard] },
  { path: 'configs/workflows', redirectTo: 'configs/movimentos', pathMatch: 'full' },
  { path: 'configs/workflows/:any', redirectTo: 'configs/movimentos' },
  { path: 'configs/workflows/:any/:any2', redirectTo: 'configs/movimentos' },
  { path: 'movimentos/estoque', component: MovimentoEstoqueListComponent, canActivate: [AuthGuard] },
  { path: 'movimentos/estoque/new', component: MovimentoEstoqueFormComponent, canActivate: [AuthGuard] },
  { path: 'movimentos/estoque/:id', component: MovimentoEstoqueFormComponent, canActivate: [AuthGuard] },
  { path: 'movimentos/estoque/:id/edit', component: MovimentoEstoqueFormComponent, canActivate: [AuthGuard] },
  { path: 'catalog/configuration', component: CatalogConfigurationPageComponent, canActivate: [AuthGuard] },
  {
    path: 'catalog/products',
    component: CatalogItemsListComponent,
    canActivate: [AuthGuard],
    data: { type: 'PRODUCTS', title: 'Produtos', singular: 'produto' }
  },
  {
    path: 'catalog/products/new',
    component: CatalogItemFormComponent,
    canActivate: [AuthGuard],
    data: { type: 'PRODUCTS', title: 'Produtos', singular: 'produto' }
  },
  {
    path: 'catalog/products/groups',
    component: CatalogGroupsPageComponent,
    canActivate: [AuthGuard],
    data: { type: 'PRODUCTS', title: 'Produtos' }
  },
  {
    path: 'catalog/products/:id/edit',
    component: CatalogItemFormComponent,
    canActivate: [AuthGuard],
    data: { type: 'PRODUCTS', title: 'Produtos', singular: 'produto' }
  },
  {
    path: 'catalog/products/:id',
    component: CatalogItemFormComponent,
    canActivate: [AuthGuard],
    data: { type: 'PRODUCTS', title: 'Produtos', singular: 'produto' }
  },
  {
    path: 'catalog/services',
    component: CatalogItemsListComponent,
    canActivate: [AuthGuard],
    data: { type: 'SERVICES', title: 'Servicos', singular: 'servico' }
  },
  {
    path: 'catalog/services/new',
    component: CatalogItemFormComponent,
    canActivate: [AuthGuard],
    data: { type: 'SERVICES', title: 'Servicos', singular: 'servico' }
  },
  {
    path: 'catalog/services/groups',
    component: CatalogGroupsPageComponent,
    canActivate: [AuthGuard],
    data: { type: 'SERVICES', title: 'Servicos' }
  },
  {
    path: 'catalog/services/:id/edit',
    component: CatalogItemFormComponent,
    canActivate: [AuthGuard],
    data: { type: 'SERVICES', title: 'Servicos', singular: 'servico' }
  },
  {
    path: 'catalog/services/:id',
    component: CatalogItemFormComponent,
    canActivate: [AuthGuard],
    data: { type: 'SERVICES', title: 'Servicos', singular: 'servico' }
  },
  { path: 'entity-types', component: EntityTypesListComponent, canActivate: [AuthGuard] },
  { path: 'entity-types/new', component: EntityTypeFormComponent, canActivate: [AuthGuard] },
  { path: 'entity-types/:id', component: EntityTypeFormComponent, canActivate: [AuthGuard] },
  { path: 'entity-types/:id/edit', component: EntityTypeFormComponent, canActivate: [AuthGuard] },
  { path: 'entities', component: EntityRecordsListComponent, canActivate: [AuthGuard] },
  { path: 'entities/groups', component: EntityGroupsPageComponent, canActivate: [AuthGuard] },
  { path: 'entities/new', component: EntityRecordFormComponent, canActivate: [AuthGuard] },
  { path: 'entities/:id', component: EntityRecordFormComponent, canActivate: [AuthGuard] },
  { path: 'entities/:id/edit', component: EntityRecordFormComponent, canActivate: [AuthGuard] },
  { path: 'help', component: HelpComponent, canActivate: [AuthGuard] },
  { path: 'blocked', component: BlockedComponent }
];
