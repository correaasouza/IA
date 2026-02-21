package com.ia.app.workflow.dto;

import java.time.Instant;
import java.util.List;

public record WorkflowRuntimeStateResponse(
  Long instanceId,
  String origin,
  Long entityId,
  String currentStateKey,
  String currentStateName,
  String currentStateColor,
  Integer definitionVersion,
  Instant updatedAt,
  List<WorkflowAvailableTransitionResponse> availableTransitions
) {}
