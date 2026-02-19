package com.ia.app.workflow.dto;

import java.time.Instant;

public record WorkflowHistoryResponse(
  Long id,
  String origin,
  Long entityId,
  String fromState,
  String toState,
  String transitionKey,
  String triggeredBy,
  Instant triggeredAt,
  String notes,
  String actionResultsJson,
  boolean success
) {}
