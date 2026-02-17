package com.ia.app.dto;

import com.ia.app.domain.MovimentoTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MovimentoConfigRequest(
  @NotNull MovimentoTipo tipoMovimento,
  @NotBlank @Size(max = 120) String nome,
  @Size(max = 120) String contextoKey,
  Boolean ativo,
  List<@NotNull Long> empresaIds,
  List<@NotNull Long> tiposEntidadePermitidos,
  Long tipoEntidadePadraoId
) {}
