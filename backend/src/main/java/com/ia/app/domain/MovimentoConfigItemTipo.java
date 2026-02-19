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
  name = "movimento_config_item_tipo",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_mov_config_item_tipo_scope",
      columnNames = {"tenant_id", "movimento_config_id", "movimento_item_tipo_id"})
  })
public class MovimentoConfigItemTipo extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "movimento_config_id", referencedColumnName = "id", nullable = false)
  private MovimentoConfig movimentoConfig;

  @Column(name = "movimento_item_tipo_id", nullable = false)
  private Long movimentoItemTipoId;

  @Column(name = "cobrar", nullable = false)
  private boolean cobrar = true;

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

  public Long getMovimentoItemTipoId() {
    return movimentoItemTipoId;
  }

  public void setMovimentoItemTipoId(Long movimentoItemTipoId) {
    this.movimentoItemTipoId = movimentoItemTipoId;
  }

  public boolean isCobrar() {
    return cobrar;
  }

  public void setCobrar(boolean cobrar) {
    this.cobrar = cobrar;
  }
}
