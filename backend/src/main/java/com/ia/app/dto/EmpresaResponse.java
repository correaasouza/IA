package com.ia.app.dto;

public record EmpresaResponse(
  Long id,
  Long tenantId,
  String tipo,
  Long matrizId,
  String razaoSocial,
  String nomeFantasia,
  String cnpj,
  boolean ativo,
  boolean padrao
) {}
