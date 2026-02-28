package com.ia.app.dto;

public record EntidadeContatoRequest(
  String nome,
  String cargo,
  Long version
) {}
