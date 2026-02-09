package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "entidade_definicao")
public class EntidadeDefinicao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "codigo", nullable = false, length = 40)
  private String codigo;

  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  @Column(name = "role_required", length = 120)
  private String roleRequired;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigo) {
    this.codigo = codigo;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }

  public String getRoleRequired() {
    return roleRequired;
  }

  public void setRoleRequired(String roleRequired) {
    this.roleRequired = roleRequired;
  }
}
