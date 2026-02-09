package com.ia.app.mapper;

import com.ia.app.domain.EntidadeRegistro;
import com.ia.app.dto.EntidadeRegistroResponse;

public class EntidadeRegistroMapper {
  private EntidadeRegistroMapper() {}

  public static EntidadeRegistroResponse toResponse(EntidadeRegistro entity) {
    return new EntidadeRegistroResponse(
      entity.getId(),
      entity.getEntidadeDefinicaoId(),
      entity.getNome(),
      entity.getApelido(),
      entity.getCpfCnpj(),
      entity.isAtivo()
    );
  }
}
