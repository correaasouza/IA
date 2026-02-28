import { of, throwError } from 'rxjs';
import { SimpleChange } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { AgrupadoresEmpresaComponent } from './agrupadores-empresa.component';
import { AgrupadorEmpresaService } from './agrupador-empresa.service';
import { CompanyService } from '../companies/company.service';
import { NotificationService } from '../../core/notifications/notification.service';

describe('AgrupadoresEmpresaComponent', () => {
  let agrupadorService: jasmine.SpyObj<AgrupadorEmpresaService>;
  let companyService: jasmine.SpyObj<CompanyService>;
  let notify: jasmine.SpyObj<NotificationService>;
  let dialog: jasmine.SpyObj<MatDialog>;
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
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);

    agrupadorService.list.and.returnValue(of([]));
    companyService.list.and.returnValue(of({ content: [], totalPages: 1 }));

    component = new AgrupadoresEmpresaComponent(agrupadorService, companyService, notify, dialog);
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

  it('should call remove service when deleting an agrupador', () => {
    const group = { id: 7, nome: 'Grupo X', ativo: true, empresas: [] };
    agrupadorService.remove.and.returnValue(of(void 0));

    component.remove(group);

    expect(agrupadorService.remove).toHaveBeenCalledWith('FORMULARIO', 10, 7);
    expect(notify.success).toHaveBeenCalledWith('Configuracao removida.');
  });
});
