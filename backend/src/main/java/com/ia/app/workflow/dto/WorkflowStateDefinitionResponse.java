package com.ia.app.workflow.dto;

public record WorkflowStateDefinitionResponse(
  Long id,
  String key,
  String name,
  String color,
  boolean isInitial,
  boolean isFinal,
  Integer uiX,
  Integer uiY,
  String metadataJson
) {}
