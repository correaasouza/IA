package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contato_tipo_por_entidade")
public class ContatoTipoPorEntidade extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "entidade_definicao_id", nullable = false)
  private Long entidadeDefinicaoId;

  @Column(name = "contato_tipo_id", nullable = false)
  private Long contatoTipoId;

  @Column(name = "obrigatorio", nullable = false)
  private boolean obrigatorio = false;

  @Column(name = "principal_unico", nullable = false)
  private boolean principalUnico = true;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getEntidadeDefinicaoId() {
    return entidadeDefinicaoId;
  }

  public void setEntidadeDefinicaoId(Long entidadeDefinicaoId) {
    this.entidadeDefinicaoId = entidadeDefinicaoId;
  }

  public Long getContatoTipoId() {
    return contatoTipoId;
  }

  public void setContatoTipoId(Long contatoTipoId) {
    this.contatoTipoId = contatoTipoId;
  }

  public boolean isObrigatorio() {
    return obrigatorio;
  }

  public void setObrigatorio(boolean obrigatorio) {
    this.obrigatorio = obrigatorio;
  }

  public boolean isPrincipalUnico() {
    return principalUnico;
  }

  public void setPrincipalUnico(boolean principalUnico) {
    this.principalUnico = principalUnico;
  }
}
