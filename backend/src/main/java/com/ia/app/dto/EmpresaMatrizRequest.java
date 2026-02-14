package com.ia.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaMatrizRequest(
  @NotBlank @Size(max = 200) String razaoSocial,
  @Size(max = 200) String nomeFantasia,
  @NotBlank @Pattern(regexp = "\\d{14}") String cnpj,
  @NotNull Boolean ativo
) {}
