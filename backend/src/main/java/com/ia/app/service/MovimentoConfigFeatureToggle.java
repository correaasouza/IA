package com.ia.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MovimentoConfigFeatureToggle {

  @Value("${movimento.config.enabled:true}")
  private boolean enabled;

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isStrictEnabled() {
    return true;
  }

  public void assertEnabled() {
    if (!enabled) {
      throw new IllegalStateException("movimento_config_feature_disabled");
    }
  }
}
