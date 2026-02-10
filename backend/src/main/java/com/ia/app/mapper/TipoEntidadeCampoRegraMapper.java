package com.ia.app.mapper;

import com.ia.app.domain.TipoEntidadeCampoRegra;
import com.ia.app.dto.TipoEntidadeCampoRegraResponse;

public class TipoEntidadeCampoRegraMapper {
  private TipoEntidadeCampoRegraMapper() {}

  public static TipoEntidadeCampoRegraResponse toResponse(TipoEntidadeCampoRegra entity) {
    return new TipoEntidadeCampoRegraResponse(
      entity.getId(),
      entity.getCampo(),
      entity.isHabilitado(),
      entity.isRequerido(),
      entity.isVisivel(),
      entity.isEditavel(),
      entity.getLabel()
    );
  }
}
