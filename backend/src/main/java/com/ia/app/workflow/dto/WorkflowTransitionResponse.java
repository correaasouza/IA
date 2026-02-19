package com.ia.app.workflow.dto;

import java.time.Instant;
import java.util.List;

public record WorkflowTransitionResponse(
  Long instanceId,
  String origin,
  Long entityId,
  String fromState,
  String toState,
  String transitionKey,
  Instant changedAt,
  String changedBy,
  List<WorkflowActionExecutionResultResponse> actions
) {}
