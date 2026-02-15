package com.ia.app.security;

public enum PermissaoCodigo {
  CONFIG_EDITOR("Configurar colunas e formulários"),
  USUARIO_MANAGE("Gerenciar usuários"),
  PAPEL_MANAGE("Gerenciar papéis"),
  RELATORIO_VIEW("Visualizar relatórios"),
  ENTIDADE_EDIT("Editar entidades");

  private final String label;

  PermissaoCodigo(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}

