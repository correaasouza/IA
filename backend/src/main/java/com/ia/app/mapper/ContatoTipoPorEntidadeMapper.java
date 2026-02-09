package com.ia.app.mapper;

import com.ia.app.domain.ContatoTipoPorEntidade;
import com.ia.app.dto.ContatoTipoPorEntidadeResponse;

public class ContatoTipoPorEntidadeMapper {
  private ContatoTipoPorEntidadeMapper() {}

  public static ContatoTipoPorEntidadeResponse toResponse(ContatoTipoPorEntidade entity) {
    return new ContatoTipoPorEntidadeResponse(
      entity.getId(),
      entity.getEntidadeDefinicaoId(),
      entity.getContatoTipoId(),
      entity.isObrigatorio(),
      entity.isPrincipalUnico()
    );
  }
}
