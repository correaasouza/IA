package com.ia.app.workflow.repository;

import com.ia.app.workflow.domain.WorkflowInstance;
import com.ia.app.workflow.domain.WorkflowOrigin;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

  Optional<WorkflowInstance> findByTenantIdAndOriginAndEntityId(Long tenantId, WorkflowOrigin origin, Long entityId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<WorkflowInstance> findWithLockByTenantIdAndOriginAndEntityId(Long tenantId, WorkflowOrigin origin, Long entityId);
}
