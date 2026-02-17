package com.ia.app.domain;

import java.util.Locale;

public enum MovimentoTipo {
  MOVIMENTO_ESTOQUE("Movimento de Estoque"),
  SOLICITACAO_ORCAMENTO_COMPRA("Solicitacao de Orcamento de Compra"),
  COTACAO_COMPRA("Cotacao de Compra"),
  ORCAMENTO_VENDA("Orcamento de Venda"),
  ORCAMENTO_VEICULAR("Orcamento Veicular"),
  ORCAMENTO_EQUIPAMENTO("Orcamento de Equipamento"),
  ORDEM_COMPRA("Ordem de Compra"),
  PEDIDO_VENDA("Pedido de Venda"),
  PEDIDO_VEICULAR("Pedido Veicular"),
  PEDIDO_EQUIPAMENTO("Pedido Equipamento"),
  CONTRATO_LOCACAO("Contrato de Locacao"),
  NOTA_FISCAL_ENTRADA("Nota Fiscal de Entrada"),
  NOTA_FISCAL_SAIDA("Nota Fiscal de Saida");

  private final String descricao;

  MovimentoTipo(String descricao) {
    this.descricao = descricao;
  }

  public String descricao() {
    return descricao;
  }

  public static MovimentoTipo from(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new IllegalArgumentException("movimento_tipo_invalid");
    }
    try {
      return MovimentoTipo.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("movimento_tipo_invalid");
    }
  }
}
