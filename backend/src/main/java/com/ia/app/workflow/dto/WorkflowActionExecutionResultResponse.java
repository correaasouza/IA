package com.ia.app.workflow.dto;

public record WorkflowActionExecutionResultResponse(
  String type,
  String status,
  String executionKey,
  String resultJson,
  String errorMessage
) {}
