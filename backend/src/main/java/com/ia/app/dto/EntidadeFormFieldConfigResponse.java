package com.ia.app.dto;

public record EntidadeFormFieldConfigResponse(
  Long id,
  String fieldKey,
  String label,
  Integer ordem,
  boolean visible,
  boolean editable,
  boolean required
) {}

