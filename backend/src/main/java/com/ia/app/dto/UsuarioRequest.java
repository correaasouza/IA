package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UsuarioRequest(
  @NotBlank String username,
  String email,
  @NotBlank String password,
  @NotNull Boolean ativo,
  @NotEmpty List<String> roles
) {}
