package com.ia.app.workflow.dto;

public record WorkflowStateDefinitionRequest(
  String key,
  String name,
  String color,
  Boolean isInitial,
  Boolean isFinal,
  Integer uiX,
  Integer uiY,
  String metadataJson
) {}
