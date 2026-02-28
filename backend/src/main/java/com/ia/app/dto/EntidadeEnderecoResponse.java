package com.ia.app.dto;

import java.math.BigDecimal;

public record EntidadeEnderecoResponse(
  Long id,
  Long registroEntidadeId,
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
  boolean principal,
  BigDecimal longitude,
  BigDecimal latitude,
  String estadoProvinciaRegiaoEstrangeiro,
  Long version
) {}
