package com.ia.app.workflow.web;

import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.dto.WorkflowHistoryResponse;
import com.ia.app.workflow.dto.WorkflowRuntimeStateResponse;
import com.ia.app.workflow.dto.WorkflowTransitionRequest;
import com.ia.app.workflow.dto.WorkflowTransitionResponse;
import com.ia.app.workflow.service.WorkflowFeatureToggle;
import com.ia.app.workflow.service.WorkflowHistoryService;
import com.ia.app.workflow.service.WorkflowRuntimeService;
import com.ia.app.workflow.service.WorkflowTransitionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows/runtime")
public class WorkflowRuntimeController {

  private final WorkflowFeatureToggle featureToggle;
  private final WorkflowRuntimeService runtimeService;
  private final WorkflowTransitionService transitionService;
  private final WorkflowHistoryService historyService;

  public WorkflowRuntimeController(
      WorkflowFeatureToggle featureToggle,
      WorkflowRuntimeService runtimeService,
      WorkflowTransitionService transitionService,
      WorkflowHistoryService historyService) {
    this.featureToggle = featureToggle;
    this.runtimeService = runtimeService;
    this.transitionService = transitionService;
    this.historyService = historyService;
  }

  @GetMapping("/{origin}/{entityId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('WORKFLOW_TRANSICIONAR')")
  public ResponseEntity<WorkflowRuntimeStateResponse> getRuntimeState(
      @PathVariable String origin,
      @PathVariable Long entityId) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(runtimeService.getRuntimeState(WorkflowOrigin.from(origin), entityId));
  }

  @PostMapping("/{origin}/{entityId}/transition")
  @PreAuthorize("@permissaoGuard.hasPermissao('WORKFLOW_TRANSICIONAR')")
  public ResponseEntity<WorkflowTransitionResponse> transition(
      @PathVariable String origin,
      @PathVariable Long entityId,
      @RequestBody WorkflowTransitionRequest request) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(transitionService.transition(WorkflowOrigin.from(origin), entityId, request));
  }

  @GetMapping("/{origin}/{entityId}/history")
  @PreAuthorize("@permissaoGuard.hasPermissao('WORKFLOW_TRANSICIONAR')")
  public ResponseEntity<Page<WorkflowHistoryResponse>> history(
      @PathVariable String origin,
      @PathVariable Long entityId,
      Pageable pageable) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(historyService.listByEntity(WorkflowOrigin.from(origin), entityId, pageable));
  }

  @PostMapping("/action-executions/{id}/retry")
  @PreAuthorize("@permissaoGuard.hasPermissao('WORKFLOW_TRANSICIONAR')")
  public ResponseEntity<Void> retryActionExecution(@PathVariable Long id) {
    featureToggle.assertEnabled();
    throw new UnsupportedOperationException("workflow_retry_not_implemented");
  }
}
