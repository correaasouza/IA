package com.ia.app.workflow.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.domain.CatalogStockAdjustment;
import com.ia.app.domain.MovimentoEstoque;
import com.ia.app.domain.MovimentoEstoqueItem;
import com.ia.app.repository.CatalogStockAdjustmentRepository;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import com.ia.app.repository.MovimentoEstoqueRepository;
import com.ia.app.service.CatalogMovementEngine;
import com.ia.app.service.CatalogUnitLockService;
import com.ia.app.workflow.domain.WorkflowDefinition;
import com.ia.app.workflow.domain.WorkflowInstance;
import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.domain.WorkflowTransition;
import com.ia.app.workflow.dto.WorkflowActionConfigRequest;
import com.ia.app.workflow.engine.WorkflowActionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MoveStockActionTest {

  @Mock
  private MovimentoEstoqueItemRepository itemRepository;

  @Mock
  private MovimentoEstoqueRepository movimentoRepository;

  @Mock
  private CatalogStockAdjustmentRepository stockAdjustmentRepository;

  @Mock
  private CatalogMovementEngine catalogMovementEngine;

  @Mock
  private CatalogUnitLockService catalogUnitLockService;

  private MoveStockAction action;

  @BeforeEach
  void setup() {
    action = new MoveStockAction(
      itemRepository,
      movimentoRepository,
      stockAdjustmentRepository,
      catalogMovementEngine,
      catalogUnitLockService,
      new ObjectMapper());
  }

  @Test
  void shouldUseMovimentoEstoqueAsMainOriginAndKeepWorkflowMetadata() {
    Long tenantId = 1L;
    Long itemId = 11L;
    Long movimentoId = 99L;

    MovimentoEstoqueItem item = new MovimentoEstoqueItem();
    item.setTenantId(tenantId);
    item.setMovimentoEstoqueId(movimentoId);
    item.setCatalogType(CatalogConfigurationType.PRODUCTS);
    item.setCatalogItemId(123L);
    item.setQuantidade(new java.math.BigDecimal("2.000000"));
    item.setQuantidadeConvertidaBase(new java.math.BigDecimal("2.000000"));
    item.setValorTotal(new java.math.BigDecimal("40.000000"));

    MovimentoEstoque movimento = new MovimentoEstoque();
    setMovimentoId(movimento, movimentoId);
    movimento.setTenantId(tenantId);
    movimento.setCodigo(7001L);
    movimento.setStockAdjustmentId(321L);

    CatalogStockAdjustment adjustment = new CatalogStockAdjustment();
    adjustment.setTenantId(tenantId);
    adjustment.setCatalogConfigurationId(10L);
    adjustment.setTipo("ENTRADA");
    adjustment.setEstoqueDestinoAgrupadorId(50L);
    adjustment.setEstoqueDestinoTipoId(60L);
    adjustment.setEstoqueDestinoFilialId(70L);

    WorkflowInstance instance = new WorkflowInstance();
    instance.setOrigin(WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE);
    instance.setEntityId(itemId);
    WorkflowDefinition definition = new WorkflowDefinition();
    definition.setVersionNum(1);
    instance.setDefinition(definition);
    instance.setDefinitionVersion(3);

    WorkflowTransition transition = new WorkflowTransition();
    transition.setTransitionKey("FINALIZAR");

    WorkflowActionContext context = new WorkflowActionContext(
      tenantId,
      "tester",
      instance,
      transition,
      "ok");

    when(itemRepository.findWithLockByIdAndTenantId(itemId, tenantId)).thenReturn(java.util.Optional.of(item));
    when(movimentoRepository.findByIdAndTenantId(movimentoId, tenantId)).thenReturn(java.util.Optional.of(movimento));
    when(stockAdjustmentRepository.findByIdAndTenantIdAndActiveTrue(321L, tenantId)).thenReturn(java.util.Optional.of(adjustment));
    when(catalogMovementEngine.apply(any())).thenReturn(new CatalogMovementEngine.Result(999L, false));

    action.execute(context, new WorkflowActionConfigRequest(null, null, null, null));

    ArgumentCaptor<CatalogMovementEngine.Command> commandCaptor = ArgumentCaptor.forClass(CatalogMovementEngine.Command.class);
    verify(catalogMovementEngine).apply(commandCaptor.capture());
    CatalogMovementEngine.Command captured = commandCaptor.getValue();

    assertThat(captured.origemTipo()).isEqualTo(CatalogMovementOriginType.MOVIMENTO_ESTOQUE);
    assertThat(captured.origemCodigo()).isEqualTo("7001");
    assertThat(captured.origemId()).isEqualTo(movimentoId);
    assertThat(captured.movimentoTipo()).isEqualTo("ENTRADA");
    assertThat(captured.workflowOrigin()).isEqualTo(WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE.name());
    assertThat(captured.workflowEntityId()).isEqualTo(itemId);
    assertThat(captured.workflowTransitionKey()).isEqualTo("FINALIZAR");
  }

  private void setMovimentoId(MovimentoEstoque movimento, Long id) {
    try {
      var field = com.ia.app.domain.MovimentoBase.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(movimento, id);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
