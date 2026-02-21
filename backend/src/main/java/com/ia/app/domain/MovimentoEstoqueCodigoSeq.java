package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
  name = "movimento_estoque_codigo_seq",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_movimento_estoque_codigo_seq_scope",
      columnNames = {"tenant_id", "movimento_config_id"})
  })
public class MovimentoEstoqueCodigoSeq extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "movimento_config_id", nullable = false)
  private Long movimentoConfigId;

  @Column(name = "next_value", nullable = false)
  private Long nextValue;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getMovimentoConfigId() {
    return movimentoConfigId;
  }

  public void setMovimentoConfigId(Long movimentoConfigId) {
    this.movimentoConfigId = movimentoConfigId;
  }

  public Long getNextValue() {
    return nextValue;
  }

  public void setNextValue(Long nextValue) {
    this.nextValue = nextValue;
  }
}
