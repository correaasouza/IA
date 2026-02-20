package com.ia.app.workflow.repository;

import com.ia.app.workflow.domain.WorkflowInstance;
import com.ia.app.workflow.domain.WorkflowOrigin;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

  Optional<WorkflowInstance> findByTenantIdAndOriginAndEntityId(Long tenantId, WorkflowOrigin origin, Long entityId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<WorkflowInstance> findWithLockByTenantIdAndOriginAndEntityId(Long tenantId, WorkflowOrigin origin, Long entityId);

  List<WorkflowInstance> findAllByTenantIdAndOriginAndEntityIdIn(Long tenantId, WorkflowOrigin origin, Collection<Long> entityIds);

  void deleteAllByTenantIdAndOriginAndEntityIdIn(Long tenantId, WorkflowOrigin origin, Collection<Long> entityIds);
}
