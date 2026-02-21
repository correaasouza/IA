package com.ia.app.workflow.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.domain.MovimentoEstoqueItem;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import com.ia.app.security.PermissaoGuard;
import com.ia.app.service.MovimentoItemBatchService;
import com.ia.app.workflow.domain.WorkflowActionType;
import com.ia.app.workflow.domain.WorkflowExecutionStatus;
import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.dto.WorkflowActionConfigRequest;
import com.ia.app.workflow.engine.WorkflowActionContext;
import com.ia.app.workflow.engine.WorkflowActionResult;
import jakarta.persistence.EntityNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class UndoStockAction implements WorkflowAction {

  private final MovimentoEstoqueItemRepository itemRepository;
  private final MovimentoItemBatchService movimentoItemBatchService;
  private final PermissaoGuard permissaoGuard;
  private final ObjectMapper objectMapper;

  public UndoStockAction(
      MovimentoEstoqueItemRepository itemRepository,
      MovimentoItemBatchService movimentoItemBatchService,
      PermissaoGuard permissaoGuard,
      ObjectMapper objectMapper) {
    this.itemRepository = itemRepository;
    this.movimentoItemBatchService = movimentoItemBatchService;
    this.permissaoGuard = permissaoGuard;
    this.objectMapper = objectMapper;
  }

  @Override
  public WorkflowActionType supports() {
    return WorkflowActionType.UNDO_STOCK;
  }

  @Override
  public WorkflowActionResult execute(WorkflowActionContext context, WorkflowActionConfigRequest config) {
    if (context.instance().getOrigin() != WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE) {
      throw new IllegalArgumentException("workflow_action_undo_stock_origin_invalid");
    }
    if (!permissaoGuard.hasPermissao("MOVIMENTO_ESTOQUE_DESFAZER")) {
      throw new AccessDeniedException("workflow_action_undo_stock_permission_denied");
    }

    Long tenantId = context.tenantId();
    Long itemId = context.instance().getEntityId();
    MovimentoEstoqueItem item = itemRepository.findWithLockByIdAndTenantId(itemId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("movimento_estoque_item_not_found"));

    if (!item.isEstoqueMovimentado() || item.getEstoqueMovimentacaoId() == null || item.getEstoqueMovimentacaoId() <= 0) {
      Map<String, Object> resultData = new LinkedHashMap<>();
      resultData.put("reused", true);
      resultData.put("reason", "not_moved");
      return new WorkflowActionResult(
        WorkflowActionType.UNDO_STOCK.name(),
        WorkflowExecutionStatus.SUCCESS,
        buildExecutionKey(item, context),
        writeJson(resultData),
        null);
    }

    movimentoItemBatchService.undoStockMovement(item.getMovimentoEstoqueId(), item.getId());
    Map<String, Object> resultData = new LinkedHashMap<>();
    resultData.put("reused", false);
    resultData.put("itemId", item.getId());
    resultData.put("movementId", item.getMovimentoEstoqueId());
    return new WorkflowActionResult(
      WorkflowActionType.UNDO_STOCK.name(),
      WorkflowExecutionStatus.SUCCESS,
      buildExecutionKey(item, context),
      writeJson(resultData),
      null);
  }

  private String buildExecutionKey(MovimentoEstoqueItem item, WorkflowActionContext context) {
    return "WF:UNDO:ITEM:"
      + item.getId()
      + ":TRANS:"
      + context.transition().getTransitionKey()
      + ":DEF:"
      + context.instance().getDefinitionVersion();
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      return null;
    }
  }
}
