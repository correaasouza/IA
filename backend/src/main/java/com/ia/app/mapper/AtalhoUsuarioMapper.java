package com.ia.app.mapper;

import com.ia.app.domain.AtalhoUsuario;
import com.ia.app.dto.AtalhoUsuarioResponse;

public class AtalhoUsuarioMapper {
  private AtalhoUsuarioMapper() {}

  public static AtalhoUsuarioResponse toResponse(AtalhoUsuario entity) {
    return new AtalhoUsuarioResponse(
      entity.getId(),
      entity.getMenuId(),
      entity.getIcon(),
      entity.getOrdem()
    );
  }
}
