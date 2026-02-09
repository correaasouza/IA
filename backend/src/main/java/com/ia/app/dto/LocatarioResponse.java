package com.ia.app.dto;

import java.time.LocalDate;

public record LocatarioResponse(
  Long id,
  String nome,
  LocalDate dataLimiteAcesso,
  boolean ativo,
  boolean bloqueado
) {}
