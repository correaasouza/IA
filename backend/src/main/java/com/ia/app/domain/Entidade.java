package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "entidade")
public class Entidade extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "tipo_entidade_id", nullable = false)
  private Long tipoEntidadeId;

  @Column(name = "pessoa_id", nullable = false)
  private Long pessoaId;

  @Column(name = "alerta")
  private String alerta;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  @Column(name = "versao", nullable = false)
  private Integer versao = 1;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getTipoEntidadeId() {
    return tipoEntidadeId;
  }

  public void setTipoEntidadeId(Long tipoEntidadeId) {
    this.tipoEntidadeId = tipoEntidadeId;
  }

  public Long getPessoaId() {
    return pessoaId;
  }

  public void setPessoaId(Long pessoaId) {
    this.pessoaId = pessoaId;
  }

  public String getAlerta() {
    return alerta;
  }

  public void setAlerta(String alerta) {
    this.alerta = alerta;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }

  public Integer getVersao() {
    return versao;
  }

  public void setVersao(Integer versao) {
    this.versao = versao;
  }
}
