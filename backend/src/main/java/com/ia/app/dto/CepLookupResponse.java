package com.ia.app.dto;

public record CepLookupResponse(
  String cep,
  String logradouro,
  String bairro,
  String localidade,
  String uf,
  String ibge,
  String ufCodigoIbge,
  String source
) {}
