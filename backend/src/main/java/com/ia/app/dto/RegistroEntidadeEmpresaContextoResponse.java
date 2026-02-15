package com.ia.app.dto;

public record RegistroEntidadeEmpresaContextoResponse(
  Long empresaId,
  String empresaNome,
  Long tipoEntidadeId,
  Long agrupadorId,
  String agrupadorNome,
  Long tipoEntidadeConfigAgrupadorId,
  boolean vinculado,
  String motivo,
  String mensagem
) {}
