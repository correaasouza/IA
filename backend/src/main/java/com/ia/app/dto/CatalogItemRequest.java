package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CatalogItemRequest(
  Long codigo,
  @NotBlank @Size(max = 200) String nome,
  @Size(max = 255) String descricao,
  Long catalogGroupId,
  @NotNull Boolean ativo
) {}
