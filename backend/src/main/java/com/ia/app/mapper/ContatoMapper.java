package com.ia.app.mapper;

import com.ia.app.domain.Contato;
import com.ia.app.dto.ContatoResponse;

public class ContatoMapper {
  private ContatoMapper() {}

  public static ContatoResponse toResponse(Contato entity) {
    return new ContatoResponse(
      entity.getId(),
      entity.getEntidadeRegistroId(),
      entity.getTipo(),
      entity.getValor(),
      entity.isPrincipal()
    );
  }
}
