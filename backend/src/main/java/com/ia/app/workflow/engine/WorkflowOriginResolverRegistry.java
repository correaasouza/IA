package com.ia.app.workflow.engine;

import com.ia.app.workflow.domain.WorkflowOrigin;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class WorkflowOriginResolverRegistry {

  private final Map<WorkflowOrigin, WorkflowOriginResolver> resolvers = new EnumMap<>(WorkflowOrigin.class);

  public WorkflowOriginResolverRegistry(List<WorkflowOriginResolver> resolverList) {
    for (WorkflowOriginResolver resolver : resolverList) {
      resolvers.put(resolver.supports(), resolver);
    }
  }

  public WorkflowOriginResolver require(WorkflowOrigin origin) {
    WorkflowOriginResolver resolver = resolvers.get(origin);
    if (resolver == null) {
      throw new IllegalArgumentException("workflow_origin_not_supported");
    }
    return resolver;
  }
}
