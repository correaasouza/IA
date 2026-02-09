package com.ia.app.dto;

public record UsuarioResponse(
  Long id,
  String username,
  String email,
  boolean ativo,
  java.util.List<String> papeis
) {}
