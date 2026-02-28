package com.ia.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntidadeContratoRhRequest(
  String numero,
  LocalDate admissaoData,
  BigDecimal remuneracao,
  BigDecimal remuneracaoComplementar,
  BigDecimal bonificacao,
  Boolean sindicalizado,
  BigDecimal percentualInsalubridade,
  BigDecimal percentualPericulosidade,
  Long tipoFuncionarioId,
  Long situacaoFuncionarioId,
  Long setorId,
  Long cargoId,
  Long ocupacaoAtividadeId,
  Long version
) {}
