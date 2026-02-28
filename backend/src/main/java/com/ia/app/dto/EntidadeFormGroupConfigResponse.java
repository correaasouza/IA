package com.ia.app.dto;

import java.util.List;

public record EntidadeFormGroupConfigResponse(
  Long id,
  String groupKey,
  String label,
  Integer ordem,
  boolean enabled,
  boolean collapsedByDefault,
  List<EntidadeFormFieldConfigResponse> fields
) {}

