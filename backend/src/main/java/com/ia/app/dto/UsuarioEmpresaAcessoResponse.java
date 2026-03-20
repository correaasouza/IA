package com.ia.app.dto;

import java.util.List;

public record UsuarioEmpresaAcessoResponse(
  List<Long> empresaIds
) {}
