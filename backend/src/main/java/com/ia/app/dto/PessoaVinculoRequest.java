package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PessoaVinculoRequest(
  @NotBlank @Size(max = 200) String nome,
  @Size(max = 200) String apelido,
  @NotBlank String tipoRegistro,
  @NotBlank String registroFederal
) {}
