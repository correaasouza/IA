package com.ia.app.dto;

import java.math.BigDecimal;

public record EntidadeEnderecoRequest(
  String nome,
  String cep,
  String cepEstrangeiro,
  String pais,
  Long paisCodigoIbge,
  String uf,
  String ufCodigoIbge,
  String municipio,
  String municipioCodigoIbge,
  String logradouro,
  String logradouroTipo,
  String numero,
  String complemento,
  String enderecoTipo,
  Boolean principal,
  BigDecimal longitude,
  BigDecimal latitude,
  String estadoProvinciaRegiaoEstrangeiro,
  Long version
) {}
