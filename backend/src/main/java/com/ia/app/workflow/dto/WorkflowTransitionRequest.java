package com.ia.app.workflow.dto;

public record WorkflowTransitionRequest(
  String transitionKey,
  String notes,
  String expectedCurrentStateKey,
  String clientRequestId
) {}
