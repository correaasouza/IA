package com.ia.app.workflow.engine;

import com.ia.app.workflow.domain.WorkflowExecutionStatus;

public record WorkflowActionResult(
  String actionType,
  WorkflowExecutionStatus status,
  String executionKey,
  String resultJson,
  String errorMessage
) {}
