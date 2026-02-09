package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PapelPermissaoRequest(
  @NotNull List<String> permissoes
) {}
