package com.ia.app.workflow.repository;

import com.ia.app.workflow.domain.WorkflowDefinition;
import com.ia.app.workflow.domain.WorkflowDefinitionStatus;
import com.ia.app.workflow.domain.WorkflowOrigin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

  Optional<WorkflowDefinition> findByIdAndTenantId(Long id, Long tenantId);

  Optional<WorkflowDefinition> findByTenantIdAndOriginAndStatusAndActiveTrue(
    Long tenantId,
    WorkflowOrigin origin,
    WorkflowDefinitionStatus status);

  Optional<WorkflowDefinition> findTopByTenantIdAndOriginOrderByVersionNumDesc(Long tenantId, WorkflowOrigin origin);
}
