package com.ia.app.workflow.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.domain.MovimentoEstoque;
import com.ia.app.domain.MovimentoEstoqueItem;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import com.ia.app.repository.MovimentoEstoqueRepository;
import com.ia.app.workflow.domain.WorkflowActionType;
import com.ia.app.workflow.domain.WorkflowDefinition;
import com.ia.app.workflow.domain.WorkflowDefinitionContextType;
import com.ia.app.workflow.domain.WorkflowDefinitionStatus;
import com.ia.app.workflow.domain.WorkflowExecutionStatus;
import com.ia.app.workflow.domain.WorkflowInstance;
import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.domain.WorkflowState;
import com.ia.app.workflow.dto.WorkflowActionConfigRequest;
import com.ia.app.workflow.engine.WorkflowActionContext;
import com.ia.app.workflow.engine.WorkflowActionResult;
import com.ia.app.workflow.repository.WorkflowDefinitionRepository;
import com.ia.app.workflow.repository.WorkflowInstanceRepository;
import com.ia.app.workflow.repository.WorkflowStateRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SetItemStatusAction implements WorkflowAction {

  private final MovimentoEstoqueRepository movimentoRepository;
  private final MovimentoEstoqueItemRepository itemRepository;
  private final WorkflowDefinitionRepository definitionRepository;
  private final WorkflowStateRepository stateRepository;
  private final WorkflowInstanceRepository instanceRepository;
  private final ObjectMapper objectMapper;

  public SetItemStatusAction(
      MovimentoEstoqueRepository movimentoRepository,
      MovimentoEstoqueItemRepository itemRepository,
      WorkflowDefinitionRepository definitionRepository,
      WorkflowStateRepository stateRepository,
      WorkflowInstanceRepository instanceRepository,
      ObjectMapper objectMapper) {
    this.movimentoRepository = movimentoRepository;
    this.itemRepository = itemRepository;
    this.definitionRepository = definitionRepository;
    this.stateRepository = stateRepository;
    this.instanceRepository = instanceRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public WorkflowActionType supports() {
    return WorkflowActionType.SET_ITEM_STATUS;
  }

  @Override
  public WorkflowActionResult execute(WorkflowActionContext context, WorkflowActionConfigRequest config) {
    if (context.instance().getOrigin() != WorkflowOrigin.MOVIMENTO_ESTOQUE) {
      throw new IllegalArgumentException("workflow_action_set_item_status_origin_invalid");
    }

    Long tenantId = context.tenantId();
    Long movimentoId = context.instance().getEntityId();
    MovimentoEstoque movimento = movimentoRepository.findByIdAndTenantId(movimentoId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("movimento_estoque_not_found"));

    List<MovimentoEstoqueItem> items = itemRepository
      .findAllByTenantIdAndMovimentoEstoqueIdOrderByOrdemAscIdAsc(tenantId, movimento.getId());
    Set<Long> itemIds = items.stream()
      .map(MovimentoEstoqueItem::getId)
      .filter(id -> id != null && id > 0)
      .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    String targetStateKey = normalizeTargetStateKey(config);
    WorkflowDefinition itemDefinition = null;
    WorkflowState targetState = null;
    if (targetStateKey != null) {
      itemDefinition = findPublishedItemDefinitionForMovimentoConfig(tenantId, movimento.getMovimentoConfigId())
        .orElseThrow(() -> new IllegalArgumentException("workflow_action_item_status_target_invalid"));
      targetState = stateRepository
        .findByDefinitionIdAndStateKey(itemDefinition.getId(), targetStateKey)
        .orElseThrow(() -> new IllegalArgumentException("workflow_action_item_status_target_invalid"));
    }

    for (MovimentoEstoqueItem item : items) {
      item.setStatus(targetStateKey);
    }
    if (!items.isEmpty()) {
      itemRepository.saveAll(items);
    }

    if (!itemIds.isEmpty()) {
      if (targetStateKey == null) {
        instanceRepository.deleteAllByTenantIdAndOriginAndEntityIdIn(
          tenantId,
          WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE,
          itemIds);
      } else {
        syncItemWorkflowInstances(tenantId, itemIds, itemDefinition, targetState);
      }
    }

    Map<String, Object> resultData = new LinkedHashMap<>();
    resultData.put("movimentoId", movimento.getId());
    resultData.put("updatedItems", itemIds.size());
    resultData.put("targetStateKey", targetStateKey);
    return new WorkflowActionResult(
      WorkflowActionType.SET_ITEM_STATUS.name(),
      WorkflowExecutionStatus.SUCCESS,
      buildExecutionLabel(movimento.getId(), targetStateKey),
      writeJson(resultData),
      null);
  }

  private java.util.Optional<WorkflowDefinition> findPublishedItemDefinitionForMovimentoConfig(
      Long tenantId,
      Long movimentoConfigId) {
    if (movimentoConfigId != null && movimentoConfigId > 0) {
      java.util.Optional<WorkflowDefinition> scoped = definitionRepository
        .findByTenantIdAndOriginAndContextTypeAndContextIdAndStatusAndActiveTrue(
          tenantId,
          WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE,
          WorkflowDefinitionContextType.MOVIMENTO_CONFIG,
          movimentoConfigId,
          WorkflowDefinitionStatus.PUBLISHED);
      if (scoped.isPresent()) {
        return scoped;
      }
    }
    return definitionRepository
      .findByTenantIdAndOriginAndContextTypeIsNullAndContextIdIsNullAndStatusAndActiveTrue(
        tenantId,
        WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE,
        WorkflowDefinitionStatus.PUBLISHED);
  }

  private void syncItemWorkflowInstances(
      Long tenantId,
      Set<Long> itemIds,
      WorkflowDefinition itemDefinition,
      WorkflowState targetState) {
    if (itemDefinition == null || targetState == null) {
      return;
    }
    List<WorkflowInstance> upserts = new ArrayList<>();
    for (Long itemId : itemIds) {
      WorkflowInstance instance = instanceRepository
        .findWithLockByTenantIdAndOriginAndEntityId(tenantId, WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE, itemId)
        .orElse(null);
      if (instance == null) {
        instance = new WorkflowInstance();
        instance.setTenantId(tenantId);
        instance.setOrigin(WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE);
        instance.setEntityId(itemId);
      }
      instance.setDefinition(itemDefinition);
      instance.setDefinitionVersion(itemDefinition.getVersionNum());
      instance.setCurrentState(targetState);
      instance.setCurrentStateKey(targetState.getStateKey());
      instance.setLastTransition(null);
      upserts.add(instance);
    }
    if (!upserts.isEmpty()) {
      instanceRepository.saveAll(upserts);
    }
  }

  private String normalizeTargetStateKey(WorkflowActionConfigRequest config) {
    if (config == null || config.params() == null) {
      return null;
    }
    Object raw = config.params().get("targetStateKey");
    if (raw == null) {
      return null;
    }
    String normalized = raw.toString().trim().toUpperCase(Locale.ROOT);
    if (normalized.isBlank()) {
      return null;
    }
    return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
  }

  private String buildExecutionLabel(Long movimentoId, String targetStateKey) {
    return "WF:MOV:"
      + movimentoId
      + ":SET_ITEM_STATUS:"
      + (targetStateKey == null ? "NONE" : targetStateKey);
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      return null;
    }
  }
}
