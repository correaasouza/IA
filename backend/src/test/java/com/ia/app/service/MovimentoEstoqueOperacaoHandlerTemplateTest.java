package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.dto.MovimentoConfigResolverResponse;
import com.ia.app.dto.MovimentoEstoqueTemplateResponse;
import com.ia.app.dto.MovimentoTemplateRequest;
import com.ia.app.repository.CatalogStockAdjustmentRepository;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import com.ia.app.repository.MovimentoEstoqueRepository;
import com.ia.app.repository.TenantUnitRepository;
import com.ia.app.tenant.EmpresaContext;
import com.ia.app.tenant.TenantContext;
import com.ia.app.workflow.service.WorkflowRuntimeService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class MovimentoEstoqueOperacaoHandlerTemplateTest {

  @AfterEach
  void clearContext() {
    EmpresaContext.clear();
    TenantContext.clear();
  }

  @Test
  void shouldReturnTemplateEvenWhenOptionalSourcesFail() {
    MovimentoEstoqueRepository repository = mock(MovimentoEstoqueRepository.class);
    MovimentoEstoqueItemRepository itemRepository = mock(MovimentoEstoqueItemRepository.class);
    MovimentoEstoqueCodigoService codigoService = mock(MovimentoEstoqueCodigoService.class);
    MovimentoConfigService movimentoConfigService = mock(MovimentoConfigService.class);
    MovimentoConfigItemTipoService movimentoConfigItemTipoService = mock(MovimentoConfigItemTipoService.class);
    MovimentoEstoqueItemCatalogService movimentoEstoqueItemCatalogService = mock(MovimentoEstoqueItemCatalogService.class);
    MovimentoItemTipoService movimentoItemTipoService = mock(MovimentoItemTipoService.class);
    CatalogStockAdjustmentConfigurationService stockAdjustmentConfigurationService = mock(CatalogStockAdjustmentConfigurationService.class);
    CatalogStockAdjustmentRepository stockAdjustmentRepository = mock(CatalogStockAdjustmentRepository.class);
    WorkflowRuntimeService workflowRuntimeService = mock(WorkflowRuntimeService.class);
    TenantUnitRepository tenantUnitRepository = mock(TenantUnitRepository.class);
    MovimentoEstoqueLockService lockService = mock(MovimentoEstoqueLockService.class);
    AuditService auditService = mock(AuditService.class);

    @SuppressWarnings("unchecked")
    ObjectProvider<MovimentoConfigItemTipoService> itemTipoProvider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<MovimentoEstoqueItemCatalogService> itemCatalogProvider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<MovimentoItemTipoService> itemTipoServiceProvider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<CatalogStockAdjustmentConfigurationService> stockAdjustmentProvider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<WorkflowRuntimeService> workflowProvider = mock(ObjectProvider.class);

    when(itemTipoProvider.getIfAvailable()).thenReturn(movimentoConfigItemTipoService);
    when(itemCatalogProvider.getIfAvailable()).thenReturn(movimentoEstoqueItemCatalogService);
    when(itemTipoServiceProvider.getIfAvailable()).thenReturn(movimentoItemTipoService);
    when(stockAdjustmentProvider.getIfAvailable()).thenReturn(stockAdjustmentConfigurationService);
    when(workflowProvider.getIfAvailable()).thenReturn(workflowRuntimeService);

    when(movimentoConfigService.resolve(MovimentoTipo.MOVIMENTO_ESTOQUE, 6L, null))
      .thenReturn(new MovimentoConfigResolverResponse(11L, MovimentoTipo.MOVIMENTO_ESTOQUE, 6L, null, null, List.of()));
    when(movimentoConfigItemTipoService.listAtivosForConfig(11L)).thenThrow(new NullPointerException("broken item tipos"));
    when(stockAdjustmentConfigurationService.listByType(com.ia.app.domain.CatalogConfigurationType.PRODUCTS))
      .thenThrow(new NullPointerException("broken stock adjustments"));

    MovimentoEstoqueOperacaoHandler handler = new MovimentoEstoqueOperacaoHandler(
      repository,
      itemRepository,
      codigoService,
      movimentoConfigService,
      itemTipoProvider,
      itemCatalogProvider,
      itemTipoServiceProvider,
      stockAdjustmentProvider,
      stockAdjustmentRepository,
      workflowProvider,
      tenantUnitRepository,
      lockService,
      auditService,
      new ObjectMapper());

    TenantContext.setTenantId(3L);
    EmpresaContext.setEmpresaId(6L);

    MovimentoEstoqueTemplateResponse template = handler.buildTemplate(new MovimentoTemplateRequest(6L));

    assertThat(template).isNotNull();
    assertThat(template.movimentoConfigId()).isEqualTo(11L);
    assertThat(template.tiposItensPermitidos()).isEmpty();
    assertThat(template.stockAdjustments()).isEmpty();
  }
}
