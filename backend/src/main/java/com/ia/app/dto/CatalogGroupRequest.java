package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CatalogGroupRequest(
  @NotBlank @Size(max = 120) String nome,
  Long parentId
) {}
