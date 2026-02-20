package com.ia.app.workflow.domain;

public record WorkflowDefinitionContext(
  WorkflowDefinitionContextType type,
  Long contextId
) {

  public static WorkflowDefinitionContext none() {
    return new WorkflowDefinitionContext(null, null);
  }

  public static WorkflowDefinitionContext of(WorkflowDefinitionContextType type, Long contextId) {
    if (type == null || contextId == null || contextId <= 0) {
      return none();
    }
    return new WorkflowDefinitionContext(type, contextId);
  }

  public boolean hasContext() {
    return type != null && contextId != null && contextId > 0;
  }
}
