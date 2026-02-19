package com.ia.app.workflow.dto;

import java.util.Map;

public record WorkflowActionConfigRequest(
  String type,
  String trigger,
  Boolean requiresSuccess,
  Map<String, Object> params
) {}
