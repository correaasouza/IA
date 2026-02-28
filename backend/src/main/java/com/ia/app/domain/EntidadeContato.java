package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "entidade_contato")
public class EntidadeContato extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "empresa_id", nullable = false)
  private Long empresaId;

  @Column(name = "registro_entidade_id", nullable = false)
  private Long registroEntidadeId;

  @Column(name = "nome", length = 120)
  private String nome;

  @Column(name = "cargo", length = 120)
  private String cargo;

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
  public String getNome() { return nome; }
  public void setNome(String nome) { this.nome = nome; }
  public String getCargo() { return cargo; }
  public void setCargo(String cargo) { this.cargo = cargo; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
