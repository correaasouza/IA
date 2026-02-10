package com.ia.app.mapper;

import com.ia.app.domain.TipoEntidade;
import com.ia.app.dto.TipoEntidadeResponse;

public class TipoEntidadeMapper {
  private TipoEntidadeMapper() {}

  public static TipoEntidadeResponse toResponse(TipoEntidade entity) {
    return new TipoEntidadeResponse(
      entity.getId(),
      entity.getCodigo(),
      entity.getNome(),
      entity.isAtivo(),
      entity.getVersao()
    );
  }
}
