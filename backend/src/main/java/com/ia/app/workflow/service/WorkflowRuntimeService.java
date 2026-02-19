package com.ia.app.workflow.service;

import com.ia.app.tenant.TenantContext;
import com.ia.app.workflow.domain.WorkflowDefinition;
import com.ia.app.workflow.domain.WorkflowDefinitionStatus;
import com.ia.app.workflow.domain.WorkflowInstance;
import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.domain.WorkflowState;
import com.ia.app.workflow.dto.WorkflowAvailableTransitionResponse;
import com.ia.app.workflow.dto.WorkflowRuntimeStateResponse;
import com.ia.app.workflow.engine.WorkflowOriginResolverRegistry;
import com.ia.app.workflow.repository.WorkflowDefinitionRepository;
import com.ia.app.workflow.repository.WorkflowInstanceRepository;
import com.ia.app.workflow.repository.WorkflowStateRepository;
import com.ia.app.workflow.repository.WorkflowTransitionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowRuntimeService {

  private final WorkflowFeatureToggle featureToggle;
  private final WorkflowDefinitionRepository definitionRepository;
  private final WorkflowStateRepository stateRepository;
  private final WorkflowTransitionRepository transitionRepository;
  private final WorkflowInstanceRepository instanceRepository;
  private final WorkflowOriginResolverRegistry originResolverRegistry;

  public WorkflowRuntimeService(
      WorkflowFeatureToggle featureToggle,
      WorkflowDefinitionRepository definitionRepository,
      WorkflowStateRepository stateRepository,
      WorkflowTransitionRepository transitionRepository,
      WorkflowInstanceRepository instanceRepository,
      WorkflowOriginResolverRegistry originResolverRegistry) {
    this.featureToggle = featureToggle;
    this.definitionRepository = definitionRepository;
    this.stateRepository = stateRepository;
    this.transitionRepository = transitionRepository;
    this.instanceRepository = instanceRepository;
    this.originResolverRegistry = originResolverRegistry;
  }

  @Transactional
  public WorkflowInstance ensureInstanceForOrigin(WorkflowOrigin origin, Long entityId) {
    featureToggle.assertEnabled();
    Long tenantId = requireTenant();
    if (entityId == null || entityId <= 0) {
      throw new IllegalArgumentException("workflow_entity_id_invalid");
    }
    WorkflowInstance existing = instanceRepository.findByTenantIdAndOriginAndEntityId(tenantId, origin, entityId).orElse(null);
    if (existing != null) {
      return syncInstanceWithPublishedDefinition(tenantId, origin, existing);
    }

    WorkflowDefinition definition = definitionRepository
      .findByTenantIdAndOriginAndStatusAndActiveTrue(tenantId, origin, WorkflowDefinitionStatus.PUBLISHED)
      .orElse(null);
    if (definition == null) {
      return null;
    }

    boolean exists = originResolverRegistry.require(origin).exists(tenantId, entityId);
    if (!exists) {
      throw new EntityNotFoundException("workflow_entity_not_found");
    }

    WorkflowState initialState = stateRepository
      .findFirstByDefinitionIdAndInitialTrueOrderByIdAsc(definition.getId())
      .orElseThrow(() -> new IllegalArgumentException("workflow_initial_state_not_found"));

    WorkflowInstance instance = new WorkflowInstance();
    instance.setTenantId(tenantId);
    instance.setOrigin(origin);
    instance.setEntityId(entityId);
    instance.setDefinition(definition);
    instance.setDefinitionVersion(definition.getVersionNum());
    instance.setCurrentState(initialState);
    instance.setCurrentStateKey(initialState.getStateKey());
    try {
      instance = instanceRepository.saveAndFlush(instance);
    } catch (DataIntegrityViolationException ex) {
      instance = instanceRepository.findByTenantIdAndOriginAndEntityId(tenantId, origin, entityId).orElseThrow(() -> ex);
    }
    originResolverRegistry.require(origin).syncStatus(tenantId, entityId, initialState.getStateKey());
    return instance;
  }

  @Transactional
  public WorkflowRuntimeStateResponse getRuntimeState(WorkflowOrigin origin, Long entityId) {
    featureToggle.assertEnabled();
    requireTenant();
    WorkflowInstance ensured = ensureInstanceForOrigin(origin, entityId);
    if (ensured == null) {
      return new WorkflowRuntimeStateResponse(
        null,
        origin.name(),
        entityId,
        null,
        null,
        null,
        null,
        List.of());
    }
    Long tenantId = requireTenant();
    WorkflowInstance instance = instanceRepository.findByTenantIdAndOriginAndEntityId(tenantId, origin, entityId)
      .orElse(ensured);
    instance = syncInstanceWithPublishedDefinition(tenantId, origin, instance);
    var transitions = transitionRepository
      .findAllByDefinitionIdAndFromStateIdAndEnabledTrueOrderByPriorityAscIdAsc(
        instance.getDefinition().getId(),
        instance.getCurrentState().getId())
      .stream()
      .map(item -> new WorkflowAvailableTransitionResponse(
        item.getTransitionKey(),
        item.getName(),
        item.getToState().getStateKey(),
        item.getToState().getName()))
      .toList();
    return new WorkflowRuntimeStateResponse(
      instance.getId(),
      instance.getOrigin().name(),
      instance.getEntityId(),
      instance.getCurrentStateKey(),
      instance.getCurrentState() != null ? instance.getCurrentState().getName() : null,
      instance.getDefinitionVersion(),
      instance.getUpdatedAt(),
      transitions);
  }

  private WorkflowInstance syncInstanceWithPublishedDefinition(Long tenantId, WorkflowOrigin origin, WorkflowInstance instance) {
    if (instance == null) {
      return null;
    }
    WorkflowDefinition published = definitionRepository
      .findByTenantIdAndOriginAndStatusAndActiveTrue(tenantId, origin, WorkflowDefinitionStatus.PUBLISHED)
      .orElse(null);
    if (published == null) {
      return instance;
    }
    if (instance.getDefinition() != null && published.getId().equals(instance.getDefinition().getId())) {
      return instance;
    }

    WorkflowState targetState = null;
    String currentStateName = instance.getCurrentState() == null ? null : instance.getCurrentState().getName();
    if (currentStateName != null && !currentStateName.isBlank()) {
      targetState = stateRepository
        .findFirstByDefinitionIdAndNameIgnoreCaseOrderByIdAsc(published.getId(), currentStateName.trim())
        .orElse(null);
    }
    if (targetState == null) {
      targetState = stateRepository
        .findFirstByDefinitionIdAndInitialTrueOrderByIdAsc(published.getId())
        .orElse(null);
    }
    if (targetState == null) {
      return instance;
    }

    instance.setDefinition(published);
    instance.setDefinitionVersion(published.getVersionNum());
    instance.setCurrentState(targetState);
    instance.setCurrentStateKey(targetState.getStateKey());
    instance.setLastTransition(null);
    WorkflowInstance saved = instanceRepository.saveAndFlush(instance);
    originResolverRegistry.require(origin).syncStatus(tenantId, saved.getEntityId(), targetState.getStateKey());
    return saved;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
