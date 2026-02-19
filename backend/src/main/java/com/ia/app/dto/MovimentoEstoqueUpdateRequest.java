package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MovimentoEstoqueUpdateRequest(
  @NotNull @Positive Long empresaId,
  @NotBlank @Size(max = 120) String nome,
  @Positive Long tipoEntidadeId,
  @NotNull @PositiveOrZero Long version,
  List<MovimentoEstoqueItemRequest> itens
) {}
