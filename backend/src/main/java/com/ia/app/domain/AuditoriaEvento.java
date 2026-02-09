package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auditoria_evento")
public class AuditoriaEvento extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "tipo", nullable = false, length = 60)
  private String tipo;

  @Column(name = "entidade", nullable = false, length = 80)
  private String entidade;

  @Column(name = "entidade_id", length = 120)
  private String entidadeId;

  @Column(name = "dados")
  private String dados;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getTipo() {
    return tipo;
  }

  public void setTipo(String tipo) {
    this.tipo = tipo;
  }

  public String getEntidade() {
    return entidade;
  }

  public void setEntidade(String entidade) {
    this.entidade = entidade;
  }

  public String getEntidadeId() {
    return entidadeId;
  }

  public void setEntidadeId(String entidadeId) {
    this.entidadeId = entidadeId;
  }

  public String getDados() {
    return dados;
  }

  public void setDados(String dados) {
    this.dados = dados;
  }
}
