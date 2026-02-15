package com.ia.app.dto;

import java.util.List;

public record AgrupadorEmpresaResponse(
  Long id,
  String nome,
  boolean ativo,
  List<AgrupadorEmpresaEmpresaResponse> empresas
) {}
