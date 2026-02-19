package com.ia.app.workflow.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.workflow.domain.WorkflowActionExecution;
import com.ia.app.workflow.domain.WorkflowActionType;
import com.ia.app.workflow.domain.WorkflowExecutionStatus;
import com.ia.app.workflow.domain.WorkflowHistory;
import com.ia.app.workflow.dto.WorkflowActionConfigRequest;
import com.ia.app.workflow.repository.WorkflowActionExecutionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WorkflowActionExecutor {

  private final WorkflowActionRegistry actionRegistry;
  private final WorkflowActionExecutionRepository actionExecutionRepository;
  private final ObjectMapper objectMapper;

  public WorkflowActionExecutor(
      WorkflowActionRegistry actionRegistry,
      WorkflowActionExecutionRepository actionExecutionRepository,
      ObjectMapper objectMapper) {
    this.actionRegistry = actionRegistry;
    this.actionExecutionRepository = actionExecutionRepository;
    this.objectMapper = objectMapper;
  }

  public List<WorkflowActionResult> executeTransitionActions(
      WorkflowActionContext context,
      WorkflowHistory history) {
    List<WorkflowActionConfigRequest> actionConfigs = parseActions(context.transition().getActionsJson());
    List<WorkflowActionResult> results = new ArrayList<>();
    for (int i = 0; i < actionConfigs.size(); i += 1) {
      WorkflowActionConfigRequest config = actionConfigs.get(i);
      if (config == null) {
        continue;
      }
      if (config.trigger() != null && !"ON_TRANSITION".equalsIgnoreCase(config.trigger())) {
        continue;
      }
      WorkflowActionType actionType = WorkflowActionType.from(config.type());
      boolean requiresSuccess = config.requiresSuccess() == null || config.requiresSuccess();
      String executionKey = buildExecutionKey(context, actionType, i);

      WorkflowActionExecution persisted = actionExecutionRepository
        .findByTenantIdAndExecutionKey(context.tenantId(), executionKey)
        .orElseGet(() -> {
          WorkflowActionExecution created = new WorkflowActionExecution();
          created.setTenantId(context.tenantId());
          created.setInstance(context.instance());
          created.setHistory(history);
          created.setActionType(actionType);
          created.setExecutionKey(executionKey);
          created.setStatus(WorkflowExecutionStatus.STARTED);
          created.setAttemptCount(1);
          created.setExecutedAt(Instant.now());
          created.setExecutedBy(context.username());
          created.setRequestJson(writeJson(config));
          return actionExecutionRepository.save(created);
        });

      if (persisted.getStatus() == WorkflowExecutionStatus.SUCCESS) {
        results.add(new WorkflowActionResult(
          actionType.name(),
          WorkflowExecutionStatus.SUCCESS,
          executionKey,
          persisted.getResultJson(),
          null));
        continue;
      }

      persisted.setStatus(WorkflowExecutionStatus.STARTED);
      persisted.setAttemptCount((persisted.getAttemptCount() == null ? 0 : persisted.getAttemptCount()) + 1);
      persisted.setExecutedAt(Instant.now());
      persisted.setExecutedBy(context.username());
      persisted.setErrorMessage(null);
      actionExecutionRepository.save(persisted);

      WorkflowActionResult result;
      try {
        result = actionRegistry.require(actionType).execute(context, config);
        persisted.setStatus(WorkflowExecutionStatus.SUCCESS);
        persisted.setResultJson(result.resultJson());
      } catch (RuntimeException ex) {
        persisted.setStatus(WorkflowExecutionStatus.FAILED);
        persisted.setErrorMessage(ex.getMessage());
        actionExecutionRepository.save(persisted);
        if (requiresSuccess) {
          throw ex;
        }
        result = new WorkflowActionResult(
          actionType.name(),
          WorkflowExecutionStatus.FAILED,
          executionKey,
          null,
          ex.getMessage());
      }
      actionExecutionRepository.save(persisted);
      results.add(result);
    }
    return results;
  }

  private String buildExecutionKey(WorkflowActionContext context, WorkflowActionType actionType, int order) {
    return "WF:"
      + context.instance().getOrigin().name()
      + ":"
      + context.instance().getEntityId()
      + ":"
      + context.transition().getTransitionKey()
      + ":"
      + context.instance().getDefinitionVersion()
      + ":"
      + actionType.name()
      + ":"
      + order;
  }

  private List<WorkflowActionConfigRequest> parseActions(String actionsJson) {
    if (actionsJson == null || actionsJson.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(actionsJson, new TypeReference<List<WorkflowActionConfigRequest>>() {});
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("workflow_action_json_invalid");
    }
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
