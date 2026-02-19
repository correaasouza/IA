package com.ia.app.workflow.domain;

import java.util.Locale;

public enum WorkflowTriggerType {
  ON_TRANSITION,
  ON_ENTER_STATE;

  public static WorkflowTriggerType from(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("workflow_trigger_type_invalid");
    }
    try {
      return WorkflowTriggerType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("workflow_trigger_type_invalid");
    }
  }
}
