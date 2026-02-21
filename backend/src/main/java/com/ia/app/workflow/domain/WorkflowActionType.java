package com.ia.app.workflow.domain;

import java.util.Locale;

public enum WorkflowActionType {
  MOVE_STOCK,
  UNDO_STOCK,
  SET_ITEM_STATUS;

  public static WorkflowActionType from(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("workflow_action_type_invalid");
    }
    try {
      return WorkflowActionType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("workflow_action_type_invalid");
    }
  }
}
