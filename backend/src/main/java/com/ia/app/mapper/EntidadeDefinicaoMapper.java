package com.ia.app.mapper;

import com.ia.app.domain.EntidadeDefinicao;
import com.ia.app.dto.EntidadeDefinicaoResponse;

public class EntidadeDefinicaoMapper {
  private EntidadeDefinicaoMapper() {}

  public static EntidadeDefinicaoResponse toResponse(EntidadeDefinicao entity) {
    return new EntidadeDefinicaoResponse(
      entity.getId(),
      entity.getCodigo(),
      entity.getNome(),
      entity.isAtivo(),
      entity.getRoleRequired()
    );
  }
}
