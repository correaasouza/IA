package com.ia.app.mapper;

import com.ia.app.domain.ContatoTipo;
import com.ia.app.dto.ContatoTipoResponse;

public class ContatoTipoMapper {
  private ContatoTipoMapper() {}

  public static ContatoTipoResponse toResponse(ContatoTipo entity) {
    return new ContatoTipoResponse(
      entity.getId(),
      entity.getCodigo(),
      entity.getNome(),
      entity.isAtivo(),
      entity.isObrigatorio(),
      entity.isPrincipalUnico(),
      entity.getMascara(),
      entity.getRegexValidacao()
    );
  }
}
