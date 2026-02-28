package com.ia.app.dto;

import java.time.LocalDate;

public record EntidadeReferenciaRequest(
  String nome,
  String atividades,
  LocalDate dataInicio,
  LocalDate dataFim,
  Long version
) {}
