package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tenant_unit_conversion")
public class TenantUnitConversion extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "unidade_origem_id", nullable = false)
  private UUID unidadeOrigemId;

  @Column(name = "unidade_destino_id", nullable = false)
  private UUID unidadeDestinoId;

  @Column(name = "fator", nullable = false, precision = 24, scale = 12)
  private BigDecimal fator = BigDecimal.ONE;

  public UUID getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public UUID getUnidadeOrigemId() {
    return unidadeOrigemId;
  }

  public void setUnidadeOrigemId(UUID unidadeOrigemId) {
    this.unidadeOrigemId = unidadeOrigemId;
  }

  public UUID getUnidadeDestinoId() {
    return unidadeDestinoId;
  }

  public void setUnidadeDestinoId(UUID unidadeDestinoId) {
    this.unidadeDestinoId = unidadeDestinoId;
  }

  public BigDecimal getFator() {
    return fator;
  }

  public void setFator(BigDecimal fator) {
    this.fator = fator;
  }
}
