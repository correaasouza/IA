package com.ia.app.workflow.repository;

import com.ia.app.workflow.domain.WorkflowActionExecution;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowActionExecutionRepository extends JpaRepository<WorkflowActionExecution, Long> {

  Optional<WorkflowActionExecution> findByTenantIdAndExecutionKey(Long tenantId, String executionKey);
}
