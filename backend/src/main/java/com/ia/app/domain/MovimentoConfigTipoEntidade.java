package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
  name = "movimento_config_tipo_entidade",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_movimento_config_tipo_entidade_scope",
      columnNames = {"tenant_id", "movimento_config_id", "tipo_entidade_id"})
  })
public class MovimentoConfigTipoEntidade extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "movimento_config_id", referencedColumnName = "id", nullable = false)
  private MovimentoConfig movimentoConfig;

  @Column(name = "tipo_entidade_id", nullable = false)
  private Long tipoEntidadeId;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public MovimentoConfig getMovimentoConfig() {
    return movimentoConfig;
  }

  public void setMovimentoConfig(MovimentoConfig movimentoConfig) {
    this.movimentoConfig = movimentoConfig;
  }

  public Long getTipoEntidadeId() {
    return tipoEntidadeId;
  }

  public void setTipoEntidadeId(Long tipoEntidadeId) {
    this.tipoEntidadeId = tipoEntidadeId;
  }
}
