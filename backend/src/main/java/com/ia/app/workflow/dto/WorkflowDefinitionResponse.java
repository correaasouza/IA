package com.ia.app.workflow.dto;

import java.time.Instant;
import java.util.List;

public record WorkflowDefinitionResponse(
  Long id,
  String origin,
  String name,
  Integer versionNum,
  String status,
  String description,
  String layoutJson,
  Instant publishedAt,
  String publishedBy,
  boolean active,
  Instant createdAt,
  Instant updatedAt,
  List<WorkflowStateDefinitionResponse> states,
  List<WorkflowTransitionDefinitionResponse> transitions
) {}
