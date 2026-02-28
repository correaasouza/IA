package com.ia.app.dto;

public record EntidadeFormFieldConfigRequest(
  String fieldKey,
  String label,
  Integer ordem,
  Boolean visible,
  Boolean editable,
  Boolean required
) {}

