package com.ia.app.workflow.repository;

import com.ia.app.workflow.domain.WorkflowHistory;
import com.ia.app.workflow.domain.WorkflowOrigin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistory, Long> {

  Page<WorkflowHistory> findAllByTenantIdAndOriginAndEntityIdOrderByTriggeredAtDescIdDesc(
    Long tenantId,
    WorkflowOrigin origin,
    Long entityId,
    Pageable pageable);
}
