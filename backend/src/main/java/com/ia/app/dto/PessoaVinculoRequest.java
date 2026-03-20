package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PessoaVinculoRequest(
  @NotBlank @Size(max = 200) String nome,
  @Size(max = 200) String apelido,
  @NotBlank String tipoRegistro,
  @NotBlank String registroFederal,
  @Size(max = 20) String tipoPessoa,
  @Size(max = 30) String genero,
  @Size(max = 120) String nacionalidade,
  @Size(max = 120) String naturalidade,
  @Size(max = 30) String estadoCivil,
  @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$") String dataNascimento
) {}
