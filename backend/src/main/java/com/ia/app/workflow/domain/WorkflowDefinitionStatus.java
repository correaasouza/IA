package com.ia.app.workflow.domain;

import java.util.Locale;

public enum WorkflowDefinitionStatus {
  DRAFT,
  PUBLISHED,
  ARCHIVED;

  public static WorkflowDefinitionStatus from(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("workflow_status_invalid");
    }
    try {
      return WorkflowDefinitionStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("workflow_status_invalid");
    }
  }
}
