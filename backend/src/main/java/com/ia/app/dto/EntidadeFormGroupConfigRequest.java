package com.ia.app.dto;

import java.util.List;

public record EntidadeFormGroupConfigRequest(
  String groupKey,
  String label,
  Integer ordem,
  Boolean enabled,
  Boolean collapsedByDefault,
  List<EntidadeFormFieldConfigRequest> fields
) {}

