package com.ia.app.dto;

public record EmpresaResponse(
  Long id,
  String tipo,
  Long matrizId,
  String razaoSocial,
  String nomeFantasia,
  String cnpj,
  boolean ativo,
  boolean padrao
) {}
