package com.ia.app.workflow.repository;

import com.ia.app.workflow.domain.WorkflowDefinition;
import com.ia.app.workflow.domain.WorkflowDefinitionContextType;
import com.ia.app.workflow.domain.WorkflowDefinitionStatus;
import com.ia.app.workflow.domain.WorkflowOrigin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

  Optional<WorkflowDefinition> findByIdAndTenantId(Long id, Long tenantId);

  Optional<WorkflowDefinition> findByTenantIdAndOriginAndContextTypeAndContextIdAndStatusAndActiveTrue(
    Long tenantId,
    WorkflowOrigin origin,
    WorkflowDefinitionContextType contextType,
    Long contextId,
    WorkflowDefinitionStatus status);

  Optional<WorkflowDefinition> findByTenantIdAndOriginAndContextTypeIsNullAndContextIdIsNullAndStatusAndActiveTrue(
    Long tenantId,
    WorkflowOrigin origin,
    WorkflowDefinitionStatus status);

  Optional<WorkflowDefinition> findTopByTenantIdAndOriginAndContextTypeAndContextIdOrderByVersionNumDesc(
    Long tenantId,
    WorkflowOrigin origin,
    WorkflowDefinitionContextType contextType,
    Long contextId);

  Optional<WorkflowDefinition> findTopByTenantIdAndOriginAndContextTypeIsNullAndContextIdIsNullOrderByVersionNumDesc(
    Long tenantId,
    WorkflowOrigin origin);
}
