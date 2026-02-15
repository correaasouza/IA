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
  name = "registro_entidade_codigo_seq",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_registro_entidade_codigo_seq_scope",
      columnNames = {"tenant_id", "tipo_entidade_config_agrupador_id"})
  })
public class RegistroEntidadeCodigoSeq extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "tipo_entidade_config_agrupador_id", nullable = false)
  private Long tipoEntidadeConfigAgrupadorId;

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

  public Long getTipoEntidadeConfigAgrupadorId() {
    return tipoEntidadeConfigAgrupadorId;
  }

  public void setTipoEntidadeConfigAgrupadorId(Long tipoEntidadeConfigAgrupadorId) {
    this.tipoEntidadeConfigAgrupadorId = tipoEntidadeConfigAgrupadorId;
  }

  public Long getNextValue() {
    return nextValue;
  }

  public void setNextValue(Long nextValue) {
    this.nextValue = nextValue;
  }
}
