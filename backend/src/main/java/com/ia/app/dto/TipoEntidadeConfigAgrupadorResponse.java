package com.ia.app.dto;

public record TipoEntidadeConfigAgrupadorResponse(
  Long agrupadorId,
  String agrupadorNome,
  boolean obrigarUmTelefone,
  boolean ativo
) {}
