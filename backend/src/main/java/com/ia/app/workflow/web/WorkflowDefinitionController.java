package com.ia.app.workflow.web;

import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.dto.WorkflowDefinitionResponse;
import com.ia.app.workflow.dto.WorkflowDefinitionUpsertRequest;
import com.ia.app.workflow.dto.WorkflowImportRequest;
import com.ia.app.workflow.dto.WorkflowValidationResponse;
import com.ia.app.workflow.service.WorkflowDefinitionService;
import com.ia.app.workflow.service.WorkflowFeatureToggle;
import com.ia.app.workflow.service.WorkflowImportExportService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows/definitions")
public class WorkflowDefinitionController {

  private final WorkflowFeatureToggle featureToggle;
  private final WorkflowDefinitionService definitionService;
  private final WorkflowImportExportService importExportService;

  public WorkflowDefinitionController(
      WorkflowFeatureToggle featureToggle,
      WorkflowDefinitionService definitionService,
      WorkflowImportExportService importExportService) {
    this.featureToggle = featureToggle;
    this.definitionService = definitionService;
    this.importExportService = importExportService;
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('WORKFLOW_CONFIGURAR')")
  public ResponseEntity<WorkflowDefinitionResponse> getById(@PathVariable Long id) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(definitionService.getById(id));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('WORKFLOW_CONFIGURAR')")
  public ResponseEntity<WorkflowDefinitionResponse> create(@RequestBody WorkflowDefinitionUpsertRequest request) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(definitionService.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('WORKFLOW_CONFIGURAR')")
  public ResponseEntity<WorkflowDefinitionResponse> update(
      @PathVariable Long id,
      @RequestBody WorkflowDefinitionUpsertRequest request) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(definitionService.update(id, request));
  }

  @PostMapping("/{id}/validate")
  @PreAuthorize("@permissaoGuard.hasPermissao('WORKFLOW_CONFIGURAR')")
  public ResponseEntity<WorkflowValidationResponse> validate(
      @PathVariable Long id,
      @RequestBody(required = false) WorkflowDefinitionUpsertRequest request) {
    featureToggle.assertEnabled();
    WorkflowDefinitionUpsertRequest payload = request == null ? definitionService.getUpsertRequestById(id) : request;
    var errors = definitionService.validate(payload);
    return ResponseEntity.ok(new WorkflowValidationResponse(errors.isEmpty(), errors));
  }

  @GetMapping("/by-origin")
  @PreAuthorize("@permissaoGuard.hasPermissao('WORKFLOW_CONFIGURAR')")
  public ResponseEntity<WorkflowDefinitionResponse> getByOrigin(@RequestParam String origin) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(definitionService.getByOrigin(WorkflowOrigin.from(origin)));
  }

  @GetMapping("/{id}/export")
  @PreAuthorize("@permissaoGuard.hasPermissao('WORKFLOW_CONFIGURAR')")
  public ResponseEntity<Map<String, String>> exportDefinition(@PathVariable Long id) {
    featureToggle.assertEnabled();
    String definitionJson = importExportService.exportDefinition(id);
    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("definitionJson", definitionJson);
    return ResponseEntity.ok(payload);
  }

  @PostMapping("/import")
  @PreAuthorize("@permissaoGuard.hasPermissao('WORKFLOW_CONFIGURAR')")
  public ResponseEntity<WorkflowDefinitionResponse> importDefinition(@RequestBody WorkflowImportRequest request) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(importExportService.importDefinition(request.definitionJson()));
  }
}
