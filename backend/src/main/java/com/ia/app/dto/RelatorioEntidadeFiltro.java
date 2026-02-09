package com.ia.app.dto;

import java.time.LocalDate;

public record RelatorioEntidadeFiltro(
  Long entidadeDefinicaoId,
  LocalDate criadoDe,
  LocalDate criadoAte
) {}
