package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_entidade")
public class TipoEntidade extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  @Column(name = "codigo_seed", length = 40)
  private String codigoSeed;

  @Column(name = "tipo_padrao", nullable = false)
  private boolean tipoPadrao;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getCodigoSeed() {
    return codigoSeed;
  }

  public void setCodigoSeed(String codigoSeed) {
    this.codigoSeed = codigoSeed;
  }

  public boolean isTipoPadrao() {
    return tipoPadrao;
  }

  public void setTipoPadrao(boolean tipoPadrao) {
    this.tipoPadrao = tipoPadrao;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }
}
