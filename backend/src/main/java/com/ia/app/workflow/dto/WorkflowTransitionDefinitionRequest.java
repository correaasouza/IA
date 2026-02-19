package com.ia.app.workflow.dto;

import java.util.List;

public record WorkflowTransitionDefinitionRequest(
  String key,
  String name,
  String fromStateKey,
  String toStateKey,
  Boolean enabled,
  Integer priority,
  String uiMetaJson,
  List<WorkflowActionConfigRequest> actions
) {}
