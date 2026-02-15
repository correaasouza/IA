import { of, throwError } from 'rxjs';
import { SimpleChange } from '@angular/core';
import { AgrupadoresEmpresaComponent } from './agrupadores-empresa.component';
import { AgrupadorEmpresaService } from './agrupador-empresa.service';
import { CompanyService } from '../companies/company.service';
import { NotificationService } from '../../core/notifications/notification.service';

describe('AgrupadoresEmpresaComponent', () => {
  let agrupadorService: jasmine.SpyObj<AgrupadorEmpresaService>;
  let companyService: jasmine.SpyObj<CompanyService>;
  let notify: jasmine.SpyObj<NotificationService>;
  let component: AgrupadoresEmpresaComponent;

  beforeEach(() => {
    agrupadorService = jasmine.createSpyObj<AgrupadorEmpresaService>('AgrupadorEmpresaService', [
      'list',
      'create',
      'rename',
      'remove',
      'addEmpresa',
      'removeEmpresa'
    ]);
    companyService = jasmine.createSpyObj<CompanyService>('CompanyService', ['list']);
    notify = jasmine.createSpyObj<NotificationService>('NotificationService', ['success', 'error', 'info']);

    agrupadorService.list.and.returnValue(of([]));
    companyService.list.and.returnValue(of({ content: [], totalPages: 1 }));

    component = new AgrupadoresEmpresaComponent(agrupadorService, companyService, notify);
    component.configType = 'formulario';
    component.configId = 10;
  });

  it('should render only with valid configType and configId', () => {
    expect(component.canRender()).toBeTrue();

    component.configId = 0;
    expect(component.canRender()).toBeFalse();

    component.configId = 10;
    component.configType = '';
    expect(component.canRender()).toBeFalse();
  });

  it('should create agrupador and reload list', () => {
    agrupadorService.create.and.returnValue(of({ id: 1, nome: 'Novo', ativo: true, empresas: [] }));
    component.createNome = 'Novo';

    component.create();

    expect(agrupadorService.create).toHaveBeenCalledWith('FORMULARIO', 10, 'Novo');
    expect(agrupadorService.list).toHaveBeenCalledWith('FORMULARIO', 10);
    expect(notify.success).toHaveBeenCalled();
  });

  it('should show api message on create conflict', () => {
    agrupadorService.create.and.returnValue(
      throwError(() => ({ error: { detail: 'Ja existe agrupador com este nome para esta configuracao.' } }))
    );
    component.createNome = 'Duplicado';

    component.create();

    expect(notify.error).toHaveBeenCalledWith('Ja existe agrupador com este nome para esta configuracao.');
  });

  it('should process add and remove empresa on selection change', () => {
    const group = {
      id: 99,
      nome: 'Grupo A',
      ativo: true,
      empresas: [{ empresaId: 1, nome: 'Empresa 1' }]
    };
    agrupadorService.addEmpresa.and.returnValue(of(group));
    agrupadorService.removeEmpresa.and.returnValue(of(group));

    component.onGroupSelectionChange(group, [2]);

    expect(agrupadorService.addEmpresa).toHaveBeenCalledWith('FORMULARIO', 10, 99, 2);
    expect(agrupadorService.removeEmpresa).toHaveBeenCalledWith('FORMULARIO', 10, 99, 1);
    expect(component.selectionBusyGroupId).toBeNull();
  });

  it('should set error state when loading agrupadores fails', () => {
    agrupadorService.list.and.returnValue(throwError(() => ({ error: { detail: 'Falha ao carregar agrupadores' } })));

    component.ngOnChanges({
      configType: new SimpleChange('', 'FORMULARIO', true),
      configId: new SimpleChange(null, 10, true)
    });

    expect(component.error).toBe('Falha ao carregar agrupadores');
    expect(component.loading).toBeFalse();
  });

  it('should notify when loading empresas fails', () => {
    companyService.list.and.returnValue(throwError(() => ({ error: { detail: 'Falha ao carregar empresas' } })));

    component.ngOnChanges({
      configType: new SimpleChange('', 'FORMULARIO', true),
      configId: new SimpleChange(null, 10, true)
    });

    expect(notify.error).toHaveBeenCalledWith('Nao foi possivel carregar as empresas.');
    expect(component.loadingEmpresas).toBeFalse();
  });
});
