package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UsuarioEmpresaAcessoRequest(
  @NotNull List<Long> empresaIds
) {}
