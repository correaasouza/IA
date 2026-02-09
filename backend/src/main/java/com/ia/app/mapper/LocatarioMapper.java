package com.ia.app.mapper;

import com.ia.app.domain.Locatario;
import com.ia.app.dto.LocatarioResponse;
import java.time.LocalDate;

public class LocatarioMapper {
  private LocatarioMapper() {}

  public static LocatarioResponse toResponse(Locatario locatario) {
    boolean bloqueado = !locatario.isAtivo() || LocalDate.now().isAfter(locatario.getDataLimiteAcesso());
    return new LocatarioResponse(
      locatario.getId(),
      locatario.getNome(),
      locatario.getDataLimiteAcesso(),
      locatario.isAtivo(),
      bloqueado
    );
  }
}
