package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;

public record UsuarioUpdateRequest(
  String username,
  String email,
  @NotNull Boolean ativo
) {}
