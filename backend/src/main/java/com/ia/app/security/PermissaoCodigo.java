package com.ia.app.security;

public enum PermissaoCodigo {
  CONFIG_EDITOR("Configurar colunas e formulÃƒÂ¡rios"),
  USUARIO_MANAGE("Gerenciar usuÃƒÂ¡rios"),
  PAPEL_MANAGE("Gerenciar papÃƒÂ©is"),
  RELATORIO_VIEW("Visualizar relatÃƒÂ³rios"),
  ENTIDADE_EDIT("Editar entidades"),
  MOVIMENTO_ESTOQUE_OPERAR("Operar movimento de estoque"),
  MOVIMENTO_ITEM_CONFIGURAR("Configurar tipos de itens de movimento"),
  MOVIMENTO_ESTOQUE_ITEM_OPERAR("Operar itens no movimento de estoque"),
  MOVIMENTO_ESTOQUE_DESFAZER("Desfazer movimentacao de estoque do item"),
  CATALOG_PRICES_VIEW("Visualizar precos de catalogo"),
  CATALOG_PRICES_MANAGE("Gerenciar precos de catalogo"),
  WORKFLOW_CONFIGURAR("Configurar workflows"),
  WORKFLOW_TRANSICIONAR("Executar transicoes de workflow");

  private final String label;

  PermissaoCodigo(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}

