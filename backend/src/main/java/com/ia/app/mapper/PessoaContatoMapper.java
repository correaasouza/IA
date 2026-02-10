package com.ia.app.mapper;

import com.ia.app.domain.PessoaContato;
import com.ia.app.dto.PessoaContatoResponse;

public class PessoaContatoMapper {
  private PessoaContatoMapper() {}

  public static PessoaContatoResponse toResponse(PessoaContato entity) {
    return new PessoaContatoResponse(
      entity.getId(),
      entity.getPessoaId(),
      entity.getTipo(),
      entity.getValor(),
      entity.isPrincipal()
    );
  }
}
