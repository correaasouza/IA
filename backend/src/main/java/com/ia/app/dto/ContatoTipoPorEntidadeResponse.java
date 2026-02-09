package com.ia.app.dto;

public record ContatoTipoPorEntidadeResponse(
  Long id,
  Long entidadeDefinicaoId,
  Long contatoTipoId,
  boolean obrigatorio,
  boolean principalUnico
) {}
