package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario_empresa_preferencia")
public class UsuarioEmpresaPreferencia extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "usuario_id", nullable = false, length = 120)
  private String usuarioId;

  @Column(name = "empresa_padrao_id", nullable = false)
  private Long empresaPadraoId;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getUsuarioId() {
    return usuarioId;
  }

  public void setUsuarioId(String usuarioId) {
    this.usuarioId = usuarioId;
  }

  public Long getEmpresaPadraoId() {
    return empresaPadraoId;
  }

  public void setEmpresaPadraoId(Long empresaPadraoId) {
    this.empresaPadraoId = empresaPadraoId;
  }
}

