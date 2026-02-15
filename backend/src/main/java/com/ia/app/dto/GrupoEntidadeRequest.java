package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GrupoEntidadeRequest(
  @NotBlank @Size(max = 120) String nome,
  Long parentId
) {}
