package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AtalhoUsuarioRequest(
  @NotBlank String menuId,
  String icon,
  @NotNull Integer ordem
) {}
