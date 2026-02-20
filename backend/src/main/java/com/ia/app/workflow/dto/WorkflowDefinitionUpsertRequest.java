package com.ia.app.workflow.dto;

import java.util.List;

public record WorkflowDefinitionUpsertRequest(
  String origin,
  String contextType,
  Long contextId,
  String name,
  String description,
  String layoutJson,
  List<WorkflowStateDefinitionRequest> states,
  List<WorkflowTransitionDefinitionRequest> transitions
) {}
