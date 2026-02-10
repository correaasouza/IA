package com.ia.app.mapper;

import com.ia.app.domain.Entidade;
import com.ia.app.dto.EntidadeResponse;

public class EntidadeMapper {
  private EntidadeMapper() {}

  public static EntidadeResponse toResponse(Entidade entity) {
    return new EntidadeResponse(
      entity.getId(),
      entity.getTipoEntidadeId(),
      entity.getPessoaId(),
      entity.getAlerta(),
      entity.isAtivo()
    );
  }
}
