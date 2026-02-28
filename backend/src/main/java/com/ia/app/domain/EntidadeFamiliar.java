package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "entidade_familiar")
public class EntidadeFamiliar extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "empresa_id", nullable = false)
  private Long empresaId;

  @Column(name = "registro_entidade_id", nullable = false)
  private Long registroEntidadeId;

  @Column(name = "entidade_parente_id", nullable = false)
  private Long entidadeParenteId;

  @Column(name = "dependente", nullable = false)
  private boolean dependente;

  @Column(name = "parentesco", nullable = false, length = 20)
  private String parentesco;

  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  public Long getId() { return id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getEmpresaId() { return empresaId; }
  public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
  public Long getRegistroEntidadeId() { return registroEntidadeId; }
  public void setRegistroEntidadeId(Long registroEntidadeId) { this.registroEntidadeId = registroEntidadeId; }
  public Long getEntidadeParenteId() { return entidadeParenteId; }
  public void setEntidadeParenteId(Long entidadeParenteId) { this.entidadeParenteId = entidadeParenteId; }
  public boolean isDependente() { return dependente; }
  public void setDependente(boolean dependente) { this.dependente = dependente; }
  public String getParentesco() { return parentesco; }
  public void setParentesco(String parentesco) { this.parentesco = parentesco; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
