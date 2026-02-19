package com.ia.app.workflow.engine;

import com.ia.app.workflow.action.WorkflowAction;
import com.ia.app.workflow.domain.WorkflowActionType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WorkflowActionRegistry {

  private final Map<WorkflowActionType, WorkflowAction> actions = new EnumMap<>(WorkflowActionType.class);

  public WorkflowActionRegistry(List<WorkflowAction> actionHandlers) {
    for (WorkflowAction action : actionHandlers) {
      actions.put(action.supports(), action);
    }
  }

  public WorkflowAction require(WorkflowActionType type) {
    WorkflowAction action = actions.get(type);
    if (action == null) {
      throw new IllegalArgumentException("workflow_action_not_supported");
    }
    return action;
  }
}
