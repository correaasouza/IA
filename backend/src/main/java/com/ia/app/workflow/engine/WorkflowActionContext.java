package com.ia.app.workflow.engine;

import com.ia.app.workflow.domain.WorkflowInstance;
import com.ia.app.workflow.domain.WorkflowTransition;

public record WorkflowActionContext(
  Long tenantId,
  String username,
  WorkflowInstance instance,
  WorkflowTransition transition,
  String transitionNotes
) {}
