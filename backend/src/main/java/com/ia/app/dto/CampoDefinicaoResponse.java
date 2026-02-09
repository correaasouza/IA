package com.ia.app.dto;

public record CampoDefinicaoResponse(
  Long id,
  Long tipoEntidadeId,
  String nome,
  String label,
  String tipo,
  boolean obrigatorio,
  Integer tamanho,
  Integer versao
) {}
