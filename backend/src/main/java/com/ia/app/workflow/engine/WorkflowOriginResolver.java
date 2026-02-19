package com.ia.app.workflow.engine;

import com.ia.app.workflow.domain.WorkflowOrigin;

public interface WorkflowOriginResolver {

  WorkflowOrigin supports();

  boolean exists(Long tenantId, Long entityId);

  void syncStatus(Long tenantId, Long entityId, String stateKey);
}
