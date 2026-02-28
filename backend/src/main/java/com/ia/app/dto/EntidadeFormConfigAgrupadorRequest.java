package com.ia.app.dto;

import java.util.List;

public record EntidadeFormConfigAgrupadorRequest(
  Boolean obrigarUmTelefone,
  List<EntidadeFormGroupConfigRequest> groups
) {}

