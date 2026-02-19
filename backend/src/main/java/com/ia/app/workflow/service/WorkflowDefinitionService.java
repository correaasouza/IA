package com.ia.app.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.tenant.TenantContext;
import com.ia.app.workflow.domain.WorkflowDefinition;
import com.ia.app.workflow.domain.WorkflowDefinitionStatus;
import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.domain.WorkflowState;
import com.ia.app.workflow.domain.WorkflowTransition;
import com.ia.app.workflow.dto.WorkflowActionConfigRequest;
import com.ia.app.workflow.dto.WorkflowDefinitionResponse;
import com.ia.app.workflow.dto.WorkflowDefinitionUpsertRequest;
import com.ia.app.workflow.dto.WorkflowStateDefinitionRequest;
import com.ia.app.workflow.dto.WorkflowStateDefinitionResponse;
import com.ia.app.workflow.dto.WorkflowTransitionDefinitionRequest;
import com.ia.app.workflow.dto.WorkflowTransitionDefinitionResponse;
import com.ia.app.workflow.repository.WorkflowDefinitionRepository;
import com.ia.app.workflow.repository.WorkflowStateRepository;
import com.ia.app.workflow.repository.WorkflowTransitionRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowDefinitionService {

  private final WorkflowDefinitionRepository definitionRepository;
  private final WorkflowStateRepository stateRepository;
  private final WorkflowTransitionRepository transitionRepository;
  private final WorkflowValidationService validationService;
  private final ObjectMapper objectMapper;

  public WorkflowDefinitionService(
      WorkflowDefinitionRepository definitionRepository,
      WorkflowStateRepository stateRepository,
      WorkflowTransitionRepository transitionRepository,
      WorkflowValidationService validationService,
      ObjectMapper objectMapper) {
    this.definitionRepository = definitionRepository;
    this.stateRepository = stateRepository;
    this.transitionRepository = transitionRepository;
    this.validationService = validationService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public WorkflowDefinitionResponse getById(Long id) {
    Long tenantId = requireTenant();
    return toResponse(findByIdForTenant(id, tenantId));
  }

  @Transactional(readOnly = true)
  public WorkflowDefinitionUpsertRequest getUpsertRequestById(Long id) {
    Long tenantId = requireTenant();
    return toUpsertRequest(findByIdForTenant(id, tenantId));
  }

  @Transactional
  public WorkflowDefinitionResponse create(WorkflowDefinitionUpsertRequest request) {
    validateOrThrow(request);
    Long tenantId = requireTenant();
    WorkflowOrigin origin = WorkflowOrigin.from(request.origin());
    WorkflowDefinition definition = savePublishedVersion(tenantId, origin, request);
    return toResponse(definition);
  }

  @Transactional
  public WorkflowDefinitionResponse update(Long id, WorkflowDefinitionUpsertRequest request) {
    validateOrThrow(request);
    Long tenantId = requireTenant();
    WorkflowDefinition definition = findByIdForTenant(id, tenantId);

    WorkflowOrigin requestedOrigin = WorkflowOrigin.from(request.origin());
    if (definition.getOrigin() != requestedOrigin) {
      throw new IllegalArgumentException("workflow_definition_origin_immutable");
    }
    WorkflowDefinition saved = savePublishedVersion(tenantId, requestedOrigin, request);
    return toResponse(saved);
  }

  @Transactional(readOnly = true)
  public WorkflowDefinitionResponse getByOrigin(WorkflowOrigin origin) {
    Long tenantId = requireTenant();
    WorkflowDefinition definition = definitionRepository
      .findByTenantIdAndOriginAndStatusAndActiveTrue(tenantId, origin, WorkflowDefinitionStatus.PUBLISHED)
      .orElseThrow(() -> new EntityNotFoundException("workflow_definition_not_published"));
    return toResponse(definition);
  }

  @Transactional(readOnly = true)
  public String exportDefinition(Long id) {
    WorkflowDefinition definition = findByIdForTenant(id, requireTenant());
    WorkflowDefinitionUpsertRequest request = toUpsertRequest(definition);
    return writeJson(request);
  }

  @Transactional
  public WorkflowDefinitionResponse importDefinition(String definitionJson) {
    if (definitionJson == null || definitionJson.isBlank()) {
      throw new IllegalArgumentException("workflow_import_payload_required");
    }
    try {
      WorkflowDefinitionUpsertRequest request = objectMapper.readValue(definitionJson, WorkflowDefinitionUpsertRequest.class);
      return create(request);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("workflow_import_payload_invalid");
    }
  }

  @Transactional(readOnly = true)
  public List<String> validate(WorkflowDefinitionUpsertRequest request) {
    return validationService.validateUpsert(request);
  }

  private void persistGraph(WorkflowDefinition definition, WorkflowDefinitionUpsertRequest request) {
    Map<String, WorkflowState> stateByKey = new LinkedHashMap<>();
    List<WorkflowState> states = new ArrayList<>();
    int stateIndex = 0;
    for (WorkflowStateDefinitionRequest source : request.states() == null ? List.<WorkflowStateDefinitionRequest>of() : request.states()) {
      if (source == null) {
        stateIndex += 1;
        continue;
      }
      String stateReferenceKey = resolveStateReferenceKey(source.key(), stateIndex);
      WorkflowState state = new WorkflowState();
      state.setTenantId(definition.getTenantId());
      state.setDefinition(definition);
      state.setStateKey(generateWorkflowUuidKey());
      state.setName(normalizeOptional(source.name(), 120));
      state.setColor(normalizeOptional(source.color(), 20));
      state.setInitial(Boolean.TRUE.equals(source.isInitial()));
      state.setTerminal(Boolean.TRUE.equals(source.isFinal()));
      state.setUiX(source.uiX() == null ? 0 : source.uiX());
      state.setUiY(source.uiY() == null ? 0 : source.uiY());
      state.setMetadataJson(normalizeOptional(source.metadataJson(), 200000));
      states.add(state);
      stateByKey.put(stateReferenceKey, state);
      stateIndex += 1;
    }
    stateRepository.saveAll(states);

    List<WorkflowTransition> transitions = new ArrayList<>();
    int transitionIndex = 0;
    for (WorkflowTransitionDefinitionRequest source : request.transitions() == null ? List.<WorkflowTransitionDefinitionRequest>of() : request.transitions()) {
      if (source == null) {
        transitionIndex += 1;
        continue;
      }
      WorkflowState from = stateByKey.get(resolveStateReferenceKey(source.fromStateKey(), -1));
      WorkflowState to = stateByKey.get(resolveStateReferenceKey(source.toStateKey(), -1));
      if (from == null || to == null) {
        transitionIndex += 1;
        continue;
      }
      WorkflowTransition transition = new WorkflowTransition();
      transition.setTenantId(definition.getTenantId());
      transition.setDefinition(definition);
      transition.setTransitionKey(generateWorkflowUuidKey());
      transition.setName(normalizeOptional(source.name(), 120));
      transition.setFromState(from);
      transition.setToState(to);
      transition.setEnabled(source.enabled() == null || source.enabled());
      transition.setPriority(source.priority() == null ? 100 : source.priority());
      transition.setUiMetaJson(normalizeOptional(source.uiMetaJson(), 200000));
      transition.setActionsJson(writeJson(source.actions() == null ? List.of() : source.actions()));
      transitions.add(transition);
      transitionIndex += 1;
    }
    transitionRepository.saveAll(transitions);
  }

  private WorkflowDefinition savePublishedVersion(
      Long tenantId,
      WorkflowOrigin origin,
      WorkflowDefinitionUpsertRequest request) {
    Integer nextVersion = definitionRepository
      .findTopByTenantIdAndOriginOrderByVersionNumDesc(tenantId, origin)
      .map(def -> (def.getVersionNum() == null ? 0 : def.getVersionNum()) + 1)
      .orElse(1);

    definitionRepository
      .findByTenantIdAndOriginAndStatusAndActiveTrue(tenantId, origin, WorkflowDefinitionStatus.PUBLISHED)
      .ifPresent(current -> {
        current.setStatus(WorkflowDefinitionStatus.ARCHIVED);
        current.setActive(false);
        definitionRepository.saveAndFlush(current);
      });

    WorkflowDefinition definition = new WorkflowDefinition();
    definition.setTenantId(tenantId);
    definition.setOrigin(origin);
    definition.setName(normalizeName(request.name()));
    definition.setDescription(normalizeOptional(request.description(), 255));
    definition.setLayoutJson(normalizeOptional(request.layoutJson(), 200000));
    definition.setStatus(WorkflowDefinitionStatus.PUBLISHED);
    definition.setVersionNum(nextVersion);
    definition.setPublishedAt(Instant.now());
    definition.setPublishedBy(resolveUsername());
    definition.setActive(true);
    definition = definitionRepository.saveAndFlush(definition);
    persistGraph(definition, request);
    return definition;
  }

  private WorkflowDefinitionUpsertRequest toUpsertRequest(WorkflowDefinition definition) {
    List<WorkflowStateDefinitionRequest> states = stateRepository
      .findAllByDefinitionIdOrderByIdAsc(definition.getId())
      .stream()
      .map(state -> new WorkflowStateDefinitionRequest(
        state.getStateKey(),
        state.getName(),
        state.getColor(),
        state.isInitial(),
        state.isTerminal(),
        state.getUiX(),
        state.getUiY(),
        state.getMetadataJson()))
      .toList();

    List<WorkflowTransitionDefinitionRequest> transitions = transitionRepository
      .findAllByDefinitionIdOrderByPriorityAscIdAsc(definition.getId())
      .stream()
      .map(transition -> new WorkflowTransitionDefinitionRequest(
        transition.getTransitionKey(),
        transition.getName(),
        transition.getFromState().getStateKey(),
        transition.getToState().getStateKey(),
        transition.isEnabled(),
        transition.getPriority(),
        transition.getUiMetaJson(),
        parseActions(transition.getActionsJson())))
      .toList();

    return new WorkflowDefinitionUpsertRequest(
      definition.getOrigin().name(),
      definition.getName(),
      definition.getDescription(),
      definition.getLayoutJson(),
      states,
      transitions);
  }

  private WorkflowDefinitionResponse toResponse(WorkflowDefinition definition) {
    List<WorkflowStateDefinitionResponse> states = stateRepository
      .findAllByDefinitionIdOrderByIdAsc(definition.getId())
      .stream()
      .map(state -> new WorkflowStateDefinitionResponse(
        state.getId(),
        state.getStateKey(),
        state.getName(),
        state.getColor(),
        state.isInitial(),
        state.isTerminal(),
        state.getUiX(),
        state.getUiY(),
        state.getMetadataJson()))
      .toList();

    List<WorkflowTransitionDefinitionResponse> transitions = transitionRepository
      .findAllByDefinitionIdOrderByPriorityAscIdAsc(definition.getId())
      .stream()
      .map(transition -> new WorkflowTransitionDefinitionResponse(
        transition.getId(),
        transition.getTransitionKey(),
        transition.getName(),
        transition.getFromState().getStateKey(),
        transition.getToState().getStateKey(),
        transition.isEnabled(),
        transition.getPriority(),
        transition.getUiMetaJson(),
        parseActions(transition.getActionsJson())))
      .toList();

    return new WorkflowDefinitionResponse(
      definition.getId(),
      definition.getOrigin().name(),
      definition.getName(),
      definition.getVersionNum(),
      definition.getStatus().name(),
      definition.getDescription(),
      definition.getLayoutJson(),
      definition.getPublishedAt(),
      definition.getPublishedBy(),
      definition.isActive(),
      definition.getCreatedAt(),
      definition.getUpdatedAt(),
      states,
      transitions);
  }

  private List<WorkflowActionConfigRequest> parseActions(String actionsJson) {
    if (actionsJson == null || actionsJson.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(actionsJson, new TypeReference<List<WorkflowActionConfigRequest>>() {});
    } catch (JsonProcessingException ex) {
      return List.of();
    }
  }

  private String writeJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("workflow_json_invalid");
    }
  }

  private void validateOrThrow(WorkflowDefinitionUpsertRequest request) {
    List<String> errors = validationService.validateUpsert(request);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(String.join(",", errors));
    }
  }

  private WorkflowDefinition findByIdForTenant(Long id, Long tenantId) {
    return definitionRepository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("workflow_definition_not_found"));
  }

  private String normalizeName(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("workflow_name_required");
    }
    String normalized = value.trim();
    return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
  }

  private String normalizeOptional(String value, int maxLen) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    return normalized.length() > maxLen ? normalized.substring(0, maxLen) : normalized;
  }

  private String resolveStateReferenceKey(String rawValue, int fallbackIndex) {
    if (rawValue != null && !rawValue.isBlank()) {
      String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
      return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
    }
    if (fallbackIndex >= 0) {
      return "STATE_REF_" + fallbackIndex;
    }
    return null;
  }

  private String generateWorkflowUuidKey() {
    return UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private String resolveUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return "system";
    }
    return authentication.getName();
  }
}
