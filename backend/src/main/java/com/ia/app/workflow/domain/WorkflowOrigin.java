package com.ia.app.workflow.domain;

import java.util.Locale;

public enum WorkflowOrigin {
  MOVIMENTO_ESTOQUE,
  ITEM_MOVIMENTO_ESTOQUE;

  public static WorkflowOrigin from(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("workflow_origin_invalid");
    }
    try {
      return WorkflowOrigin.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("workflow_origin_invalid");
    }
  }
}
