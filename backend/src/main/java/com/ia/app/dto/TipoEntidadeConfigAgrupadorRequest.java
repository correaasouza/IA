package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;

public record TipoEntidadeConfigAgrupadorRequest(
  @NotNull Boolean obrigarUmTelefone
) {}
