package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LocatarioRequest(
  @NotBlank String nome,
  @NotNull LocalDate dataLimiteAcesso,
  @NotNull Boolean ativo
) {}
