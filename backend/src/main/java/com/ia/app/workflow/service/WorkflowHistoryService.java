package com.ia.app.workflow.service;

import com.ia.app.tenant.TenantContext;
import com.ia.app.workflow.domain.WorkflowOrigin;
import com.ia.app.workflow.dto.WorkflowHistoryResponse;
import com.ia.app.workflow.repository.WorkflowHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowHistoryService {

  private final WorkflowHistoryRepository historyRepository;

  public WorkflowHistoryService(WorkflowHistoryRepository historyRepository) {
    this.historyRepository = historyRepository;
  }

  @Transactional(readOnly = true)
  public Page<WorkflowHistoryResponse> listByEntity(WorkflowOrigin origin, Long entityId, Pageable pageable) {
    Long tenantId = requireTenant();
    return historyRepository
      .findAllByTenantIdAndOriginAndEntityIdOrderByTriggeredAtDescIdDesc(tenantId, origin, entityId, pageable)
      .map(item -> new WorkflowHistoryResponse(
        item.getId(),
        item.getOrigin().name(),
        item.getEntityId(),
        item.getFromStateKey(),
        item.getToStateKey(),
        item.getTransitionKey(),
        item.getTriggeredBy(),
        item.getTriggeredAt(),
        item.getNotes(),
        item.getActionResultsJson(),
        item.isSuccess()));
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
