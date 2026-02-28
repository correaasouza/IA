package com.ia.app.dto;

import java.time.LocalDate;

public record EntidadeReferenciaResponse(
  Long id,
  Long registroEntidadeId,
  String nome,
  String atividades,
  LocalDate dataInicio,
  LocalDate dataFim,
  Long version
) {}
