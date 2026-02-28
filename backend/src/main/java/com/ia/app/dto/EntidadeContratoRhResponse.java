package com.ia.app.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntidadeContratoRhResponse(
  Long id,
  Long registroEntidadeId,
  String numero,
  LocalDate admissaoData,
  BigDecimal remuneracao,
  BigDecimal remuneracaoComplementar,
  BigDecimal bonificacao,
  boolean sindicalizado,
  BigDecimal percentualInsalubridade,
  BigDecimal percentualPericulosidade,
  Long tipoFuncionarioId,
  Long situacaoFuncionarioId,
  Long setorId,
  Long cargoId,
  Long ocupacaoAtividadeId,
  Long version
) {}
