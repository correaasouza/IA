package com.ia.app.mapper;

import com.ia.app.domain.Empresa;
import com.ia.app.dto.EmpresaResponse;

public class EmpresaMapper {
  private EmpresaMapper() {}

  public static EmpresaResponse toResponse(Empresa entity) {
    return toResponse(entity, false);
  }

  public static EmpresaResponse toResponse(Empresa entity, boolean padrao) {
    return new EmpresaResponse(
      entity.getId(),
      entity.getTipo(),
      entity.getMatriz() == null ? null : entity.getMatriz().getId(),
      entity.getRazaoSocial(),
      entity.getNomeFantasia(),
      entity.getCnpj(),
      entity.isAtivo(),
      padrao
    );
  }
}
