package com.ia.app.mapper;

import com.ia.app.domain.Pessoa;
import com.ia.app.dto.PessoaResponse;

public class PessoaMapper {
  private PessoaMapper() {}

  public static PessoaResponse toResponse(Pessoa entity) {
    return new PessoaResponse(
      entity.getId(),
      entity.getNome(),
      entity.getApelido(),
      entity.getCpf(),
      entity.getCnpj(),
      entity.getIdEstrangeiro(),
      entity.isAtivo()
    );
  }
}
