package com.ia.app.security;

public enum PermissaoCodigo {
  CONFIG_EDITOR("Configurar colunas e formulários"),
  USUARIO_MANAGE("Gerenciar usuários"),
  PAPEL_MANAGE("Gerenciar papéis"),
  RELATORIO_VIEW("Visualizar relatórios"),
  ENTIDADE_EDIT("Editar entidades"),
  MOVIMENTO_ESTOQUE_OPERAR("Operar movimento de estoque"),
  MOVIMENTO_ITEM_CONFIGURAR("Configurar tipos de itens de movimento"),
  MOVIMENTO_ESTOQUE_ITEM_OPERAR("Operar itens no movimento de estoque"),
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

