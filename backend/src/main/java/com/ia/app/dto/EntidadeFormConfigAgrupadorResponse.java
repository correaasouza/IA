package com.ia.app.dto;

import java.util.List;

public record EntidadeFormConfigAgrupadorResponse(
  Long tipoEntidadeId,
  Long agrupadorId,
  String agrupadorNome,
  boolean obrigarUmTelefone,
  List<EntidadeFormGroupConfigResponse> groups
) {}

