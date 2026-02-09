package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
  @NotBlank String newPassword
) {}
