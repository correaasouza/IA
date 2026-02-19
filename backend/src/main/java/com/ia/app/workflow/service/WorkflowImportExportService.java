package com.ia.app.workflow.service;

import com.ia.app.workflow.dto.WorkflowDefinitionResponse;
import org.springframework.stereotype.Service;

@Service
public class WorkflowImportExportService {

  private final WorkflowDefinitionService definitionService;

  public WorkflowImportExportService(WorkflowDefinitionService definitionService) {
    this.definitionService = definitionService;
  }

  public String exportDefinition(Long definitionId) {
    return definitionService.exportDefinition(definitionId);
  }

  public WorkflowDefinitionResponse importDefinition(String definitionJson) {
    return definitionService.importDefinition(definitionJson);
  }
}
