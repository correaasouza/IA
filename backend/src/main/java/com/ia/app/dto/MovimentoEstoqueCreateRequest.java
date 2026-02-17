package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MovimentoEstoqueCreateRequest(
  @NotNull @Positive Long empresaId,
  @NotBlank @Size(max = 120) String nome,
  LocalDate dataMovimento
) {}
