package com.ia.app.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.service.AuditService;
import com.ia.app.tenant.TenantContext;
import com.ia.app.workflow.domain.WorkflowHistory;
import com.ia.app.workflow.domain.WorkflowInstance;
import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.domain.WorkflowTransition;
import com.ia.app.workflow.dto.WorkflowActionExecutionResultResponse;
import com.ia.app.workflow.dto.WorkflowTransitionRequest;
import com.ia.app.workflow.dto.WorkflowTransitionResponse;
import com.ia.app.workflow.engine.WorkflowActionContext;
import com.ia.app.workflow.engine.WorkflowActionExecutor;
import com.ia.app.workflow.engine.WorkflowActionResult;
import com.ia.app.workflow.engine.WorkflowOriginResolverRegistry;
import com.ia.app.workflow.repository.WorkflowHistoryRepository;
import com.ia.app.workflow.repository.WorkflowInstanceRepository;
import com.ia.app.workflow.repository.WorkflowTransitionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowTransitionService {

  private final WorkflowFeatureToggle featureToggle;
  private final WorkflowRuntimeService runtimeService;
  private final WorkflowInstanceRepository instanceRepository;
  private final WorkflowTransitionRepository transitionRepository;
  private final WorkflowHistoryRepository historyRepository;
  private final WorkflowActionExecutor actionExecutor;
  private final WorkflowOriginResolverRegistry originResolverRegistry;
  private final AuditService auditService;
  private final ObjectMapper objectMapper;

  public WorkflowTransitionService(
      WorkflowFeatureToggle featureToggle,
      WorkflowRuntimeService runtimeService,
      WorkflowInstanceRepository instanceRepository,
      WorkflowTransitionRepository transitionRepository,
      WorkflowHistoryRepository historyRepository,
      WorkflowActionExecutor actionExecutor,
      WorkflowOriginResolverRegistry originResolverRegistry,
      AuditService auditService,
      ObjectMapper objectMapper) {
    this.featureToggle = featureToggle;
    this.runtimeService = runtimeService;
    this.instanceRepository = instanceRepository;
    this.transitionRepository = transitionRepository;
    this.historyRepository = historyRepository;
    this.actionExecutor = actionExecutor;
    this.originResolverRegistry = originResolverRegistry;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public WorkflowTransitionResponse transition(WorkflowOrigin origin, Long entityId, WorkflowTransitionRequest request) {
    featureToggle.assertEnabled();
    Long tenantId = requireTenant();
    String userId = resolveUserId();
    WorkflowInstance ensured = runtimeService.ensureInstanceForOrigin(origin, entityId);
    if (ensured == null) {
      throw new IllegalStateException("workflow_definition_not_published");
    }

    WorkflowInstance instance = instanceRepository.findWithLockByTenantIdAndOriginAndEntityId(tenantId, origin, entityId)
      .orElseThrow(() -> new EntityNotFoundException("workflow_instance_not_found"));

    String expectedState = normalizeKey(request == null ? null : request.expectedCurrentStateKey());
    if (expectedState != null && !expectedState.equals(instance.getCurrentStateKey())) {
      throw new IllegalArgumentException("workflow_current_state_mismatch");
    }

    String transitionKey = normalizeKey(request == null ? null : request.transitionKey());
    WorkflowTransition transition = transitionRepository
      .findByDefinitionIdAndTransitionKey(instance.getDefinition().getId(), transitionKey)
      .orElseThrow(() -> new IllegalArgumentException("workflow_transition_not_found"));

    if (!transition.isEnabled()) {
      throw new IllegalArgumentException("workflow_transition_disabled");
    }
    if (!transition.getFromState().getId().equals(instance.getCurrentState().getId())) {
      throw new IllegalArgumentException("workflow_transition_state_invalid");
    }

    WorkflowActionContext context = new WorkflowActionContext(
      tenantId,
      userId,
      instance,
      transition,
      request == null ? null : request.notes());

    WorkflowHistory history = new WorkflowHistory();
    history.setTenantId(tenantId);
    history.setInstance(instance);
    history.setOrigin(instance.getOrigin());
    history.setEntityId(instance.getEntityId());
    history.setFromStateKey(instance.getCurrentStateKey());
    history.setToStateKey(transition.getToState().getStateKey());
    history.setTransitionKey(transition.getTransitionKey());
    history.setTriggeredBy(userId);
    history.setTriggeredAt(Instant.now());
    history.setNotes(request == null ? null : normalizeOptional(request.notes(), 500));
    history.setSuccess(true);
    history = historyRepository.save(history);

    List<WorkflowActionResult> actionResults = actionExecutor.executeTransitionActions(context, history);
    history.setActionResultsJson(writeJson(actionResults));
    historyRepository.save(history);

    instance.setCurrentState(transition.getToState());
    instance.setCurrentStateKey(transition.getToState().getStateKey());
    instance.setLastTransition(transition);
    instanceRepository.save(instance);

    originResolverRegistry.require(origin).syncStatus(tenantId, entityId, transition.getToState().getStateKey());
    auditService.log(
      tenantId,
      "WORKFLOW_TRANSITION_EXECUTED",
      "workflow_instance",
      String.valueOf(instance.getId()),
      "origin=" + origin.name() + ";entityId=" + entityId + ";transition=" + transition.getTransitionKey());

    return new WorkflowTransitionResponse(
      instance.getId(),
      origin.name(),
      entityId,
      history.getFromStateKey(),
      history.getToStateKey(),
      transition.getTransitionKey(),
      history.getTriggeredAt(),
      userId,
      actionResults.stream()
        .map(item -> new WorkflowActionExecutionResultResponse(
          item.actionType(),
          item.status().name(),
          item.executionKey(),
          item.resultJson(),
          item.errorMessage()))
        .toList());
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private String resolveUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return "system";
    }
    return authentication.getName();
  }

  private String normalizeKey(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizeOptional(String value, int maxLen) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    return normalized.length() > maxLen ? normalized.substring(0, maxLen) : normalized;
  }

  private String writeJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      return null;
    }
  }
}
