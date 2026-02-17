package com.ia.app.dto;

import com.ia.app.domain.MovimentoTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MovimentoConfigRequest(
  @NotNull MovimentoTipo tipoMovimento,
  @NotBlank @Size(max = 120) String nome,
  @Size(max = 120) String contextoKey,
  Boolean ativo,
  @NotEmpty List<@NotNull Long> empresaIds,
  @NotEmpty List<@NotNull Long> tiposEntidadePermitidos,
  @NotNull Long tipoEntidadePadraoId
) {}
