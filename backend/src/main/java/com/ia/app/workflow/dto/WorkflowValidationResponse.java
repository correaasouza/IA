package com.ia.app.workflow.dto;

import java.util.List;

public record WorkflowValidationResponse(
  boolean valid,
  List<String> errors
) {}
