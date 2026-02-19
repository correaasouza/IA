package com.ia.app.workflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkflowFeatureToggle {

  @Value("${workflow.enabled:true}")
  private boolean enabled;

  public boolean isEnabled() {
    return enabled;
  }

  public void assertEnabled() {
    if (!enabled) {
      throw new IllegalStateException("workflow_feature_disabled");
    }
  }
}
