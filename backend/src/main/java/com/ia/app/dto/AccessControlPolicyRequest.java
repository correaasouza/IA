package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AccessControlPolicyRequest(
  @NotNull List<String> roles
) {}

