package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "entidade_qualificacao_item")
public class EntidadeQualificacaoItem extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false) private Long tenantId;
  @Column(name = "empresa_id", nullable = false) private Long empresaId;
  @Column(name = "registro_entidade_id", nullable = false) private Long registroEntidadeId;
  @Column(name = "rh_qualificacao_id", nullable = false) private Long rhQualificacaoId;
  @Column(name = "completo", nullable = false) private boolean completo;
  @Column(name = "tipo", length = 1) private String tipo;

  @Version
  @Column(name = "version", nullable = false) private Long version = 0L;

  public Long getId() { return id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getEmpresaId() { return empresaId; }
  public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
  public Long getRegistroEntidadeId() { return registroEntidadeId; }
  public void setRegistroEntidadeId(Long registroEntidadeId) { this.registroEntidadeId = registroEntidadeId; }
  public Long getRhQualificacaoId() { return rhQualificacaoId; }
  public void setRhQualificacaoId(Long rhQualificacaoId) { this.rhQualificacaoId = rhQualificacaoId; }
  public boolean isCompleto() { return completo; }
  public void setCompleto(boolean completo) { this.completo = completo; }
  public String getTipo() { return tipo; }
  public void setTipo(String tipo) { this.tipo = tipo; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
