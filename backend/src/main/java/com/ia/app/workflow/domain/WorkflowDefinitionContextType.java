package com.ia.app.workflow.domain;

import java.util.Locale;

public enum WorkflowDefinitionContextType {
  MOVIMENTO_CONFIG;

  public static WorkflowDefinitionContextType from(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("workflow_context_invalid");
    }
    try {
      return WorkflowDefinitionContextType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("workflow_context_invalid");
    }
  }

  public static WorkflowDefinitionContextType fromNullable(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return from(raw);
  }
}
