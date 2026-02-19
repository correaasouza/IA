package com.ia.app.workflow.domain;

import java.util.Locale;

public enum WorkflowExecutionStatus {
  STARTED,
  SUCCESS,
  FAILED,
  PENDING;

  public static WorkflowExecutionStatus from(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("workflow_execution_status_invalid");
    }
    try {
      return WorkflowExecutionStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("workflow_execution_status_invalid");
    }
  }
}
