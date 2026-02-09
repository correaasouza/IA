package com.ia.app.mapper;

import com.ia.app.domain.CampoDefinicao;
import com.ia.app.dto.CampoDefinicaoResponse;

public class CampoDefinicaoMapper {
  private CampoDefinicaoMapper() {}

  public static CampoDefinicaoResponse toResponse(CampoDefinicao entity) {
    return new CampoDefinicaoResponse(
      entity.getId(),
      entity.getTipoEntidadeId(),
      entity.getNome(),
      entity.getLabel(),
      entity.getTipo(),
      entity.isObrigatorio(),
      entity.getTamanho(),
      entity.getVersao()
    );
  }
}
