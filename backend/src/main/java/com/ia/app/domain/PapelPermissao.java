package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "papel_permissao")
public class PapelPermissao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "papel_id", nullable = false)
  private Long papelId;

  @Column(name = "permissao_codigo", nullable = false, length = 80)
  private String permissaoCodigo;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getPapelId() {
    return papelId;
  }

  public void setPapelId(Long papelId) {
    this.papelId = papelId;
  }

  public String getPermissaoCodigo() {
    return permissaoCodigo;
  }

  public void setPermissaoCodigo(String permissaoCodigo) {
    this.permissaoCodigo = permissaoCodigo;
  }
}
