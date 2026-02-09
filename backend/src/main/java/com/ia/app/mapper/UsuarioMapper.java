package com.ia.app.mapper;

import com.ia.app.domain.Usuario;
import com.ia.app.dto.UsuarioResponse;

public class UsuarioMapper {
  private UsuarioMapper() {}

  public static UsuarioResponse toResponse(Usuario usuario) {
    return new UsuarioResponse(
      usuario.getId(),
      usuario.getUsername(),
      usuario.getEmail(),
      usuario.isAtivo(),
      java.util.List.of()
    );
  }
}
