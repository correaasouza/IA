package com.ia.app.workflow.dto;

import java.util.List;

public record WorkflowTransitionDefinitionResponse(
  Long id,
  String key,
  String name,
  String fromStateKey,
  String toStateKey,
  boolean enabled,
  Integer priority,
  String uiMetaJson,
  List<WorkflowActionConfigRequest> actions
) {}
