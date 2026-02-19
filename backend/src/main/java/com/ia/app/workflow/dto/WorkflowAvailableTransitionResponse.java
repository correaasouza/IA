package com.ia.app.workflow.dto;

public record WorkflowAvailableTransitionResponse(
  String key,
  String name,
  String toStateKey,
  String toStateName
) {}
