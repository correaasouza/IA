package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "catalog_movement_line")
public class CatalogMovementLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "movement_id", nullable = false)
  private Long movementId;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "agrupador_empresa_id", nullable = false)
  private Long agrupadorEmpresaId;

  @Enumerated(EnumType.STRING)
  @Column(name = "metric_type", nullable = false, length = 20)
  private CatalogMovementMetricType metricType;

  @Column(name = "estoque_tipo_id", nullable = false)
  private Long estoqueTipoId;

  @Column(name = "filial_id", nullable = false)
  private Long filialId;

  @Column(name = "before_value", nullable = false, precision = 19, scale = 6)
  private java.math.BigDecimal beforeValue = java.math.BigDecimal.ZERO;

  @Column(name = "delta", nullable = false, precision = 19, scale = 6)
  private java.math.BigDecimal delta = java.math.BigDecimal.ZERO;

  @Column(name = "after_value", nullable = false, precision = 19, scale = 6)
  private java.math.BigDecimal afterValue = java.math.BigDecimal.ZERO;

  public Long getId() {
    return id;
  }

  public Long getMovementId() {
    return movementId;
  }

  public void setMovementId(Long movementId) {
    this.movementId = movementId;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getAgrupadorEmpresaId() {
    return agrupadorEmpresaId;
  }

  public void setAgrupadorEmpresaId(Long agrupadorEmpresaId) {
    this.agrupadorEmpresaId = agrupadorEmpresaId;
  }

  public CatalogMovementMetricType getMetricType() {
    return metricType;
  }

  public void setMetricType(CatalogMovementMetricType metricType) {
    this.metricType = metricType;
  }

  public Long getEstoqueTipoId() {
    return estoqueTipoId;
  }

  public void setEstoqueTipoId(Long estoqueTipoId) {
    this.estoqueTipoId = estoqueTipoId;
  }

  public Long getFilialId() {
    return filialId;
  }

  public void setFilialId(Long filialId) {
    this.filialId = filialId;
  }

  public java.math.BigDecimal getBeforeValue() {
    return beforeValue;
  }

  public void setBeforeValue(java.math.BigDecimal beforeValue) {
    this.beforeValue = beforeValue;
  }

  public java.math.BigDecimal getDelta() {
    return delta;
  }

  public void setDelta(java.math.BigDecimal delta) {
    this.delta = delta;
  }

  public java.math.BigDecimal getAfterValue() {
    return afterValue;
  }

  public void setAfterValue(java.math.BigDecimal afterValue) {
    this.afterValue = afterValue;
  }
}
