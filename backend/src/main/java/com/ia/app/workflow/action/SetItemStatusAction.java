package com.ia.app.workflow.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.domain.MovimentoEstoque;
import com.ia.app.domain.MovimentoEstoqueItem;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import com.ia.app.repository.MovimentoEstoqueRepository;
import com.ia.app.workflow.domain.WorkflowActionType;
import com.ia.app.workflow.domain.WorkflowExecutionStatus;
import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.dto.WorkflowActionConfigRequest;
import com.ia.app.workflow.dto.WorkflowAvailableTransitionResponse;
import com.ia.app.workflow.dto.WorkflowRuntimeStateResponse;
import com.ia.app.workflow.dto.WorkflowTransitionRequest;
import com.ia.app.workflow.engine.WorkflowActionContext;
import com.ia.app.workflow.engine.WorkflowActionResult;
import com.ia.app.workflow.repository.WorkflowInstanceRepository;
import com.ia.app.workflow.service.WorkflowRuntimeService;
import com.ia.app.workflow.service.WorkflowTransitionService;
import jakarta.persistence.EntityNotFoundException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class SetItemStatusAction implements WorkflowAction {

  private final MovimentoEstoqueRepository movimentoRepository;
  private final MovimentoEstoqueItemRepository itemRepository;
  private final WorkflowInstanceRepository instanceRepository;
  private final ObjectProvider<WorkflowRuntimeService> runtimeServiceProvider;
  private final ObjectProvider<WorkflowTransitionService> transitionServiceProvider;
  private final ObjectMapper objectMapper;

  public SetItemStatusAction(
      MovimentoEstoqueRepository movimentoRepository,
      MovimentoEstoqueItemRepository itemRepository,
      WorkflowInstanceRepository instanceRepository,
      ObjectProvider<WorkflowRuntimeService> runtimeServiceProvider,
      ObjectProvider<WorkflowTransitionService> transitionServiceProvider,
      ObjectMapper objectMapper) {
    this.movimentoRepository = movimentoRepository;
    this.itemRepository = itemRepository;
    this.instanceRepository = instanceRepository;
    this.runtimeServiceProvider = runtimeServiceProvider;
    this.transitionServiceProvider = transitionServiceProvider;
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
    int transitionedItems = 0;
    int alreadyInTargetItems = 0;

    if (!itemIds.isEmpty()) {
      if (targetStateKey == null) {
        for (MovimentoEstoqueItem item : items) {
          item.setStatus(null);
        }
        if (!items.isEmpty()) {
          itemRepository.saveAll(items);
        }
        instanceRepository.deleteAllByTenantIdAndOriginAndEntityIdIn(
          tenantId,
          WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE,
          itemIds);
      } else {
        WorkflowRuntimeService runtimeService = requireRuntimeService();
        WorkflowTransitionService transitionService = requireTransitionService();
        for (Long itemId : itemIds) {
          WorkflowRuntimeStateResponse runtime = runtimeService.getRuntimeState(WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE, itemId);
          String currentStateKey = normalizeKey(runtime.currentStateKey());
          if (targetStateKey.equals(currentStateKey)) {
            alreadyInTargetItems += 1;
            continue;
          }

          String transitionKey = resolveTransitionKey(runtime.availableTransitions(), targetStateKey);
          if (transitionKey == null) {
            throw new IllegalArgumentException("workflow_action_item_status_target_invalid");
          }
          transitionService.transition(
            WorkflowOrigin.ITEM_MOVIMENTO_ESTOQUE,
            itemId,
            new WorkflowTransitionRequest(
              transitionKey,
              "Transicao automatica via workflow do movimento " + movimento.getId(),
              currentStateKey,
              null));
          transitionedItems += 1;
        }
      }
    }

    Map<String, Object> resultData = new LinkedHashMap<>();
    resultData.put("movimentoId", movimento.getId());
    resultData.put("updatedItems", itemIds.size());
    resultData.put("transitionedItems", transitionedItems);
    resultData.put("alreadyInTargetItems", alreadyInTargetItems);
    resultData.put("targetStateKey", targetStateKey);
    return new WorkflowActionResult(
      WorkflowActionType.SET_ITEM_STATUS.name(),
      WorkflowExecutionStatus.SUCCESS,
      buildExecutionLabel(movimento.getId(), targetStateKey),
      writeJson(resultData),
      null);
  }

  private String resolveTransitionKey(List<WorkflowAvailableTransitionResponse> transitions, String targetStateKey) {
    if (transitions == null || transitions.isEmpty() || targetStateKey == null) {
      return null;
    }
    for (WorkflowAvailableTransitionResponse transition : transitions) {
      if (transition == null) {
        continue;
      }
      String toStateKey = normalizeKey(transition.toStateKey());
      if (targetStateKey.equals(toStateKey)) {
        return transition.key();
      }
    }
    return null;
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

  private String normalizeKey(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
  }

  private WorkflowRuntimeService requireRuntimeService() {
    WorkflowRuntimeService service = runtimeServiceProvider.getIfAvailable();
    if (service == null) {
      throw new IllegalStateException("workflow_runtime_service_required");
    }
    return service;
  }

  private WorkflowTransitionService requireTransitionService() {
    WorkflowTransitionService service = transitionServiceProvider.getIfAvailable();
    if (service == null) {
      throw new IllegalStateException("workflow_transition_service_required");
    }
    return service;
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
