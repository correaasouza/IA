package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(
  name = "catalog_price_rule_by_group",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_catalog_price_rule_group_type",
      columnNames = {"tenant_id", "catalog_configuration_by_group_id", "price_type"})
  })
public class CatalogPriceRuleByGroup extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "catalog_configuration_by_group_id", nullable = false)
  private Long catalogConfigurationByGroupId;

  @Enumerated(EnumType.STRING)
  @Column(name = "price_type", nullable = false, length = 20)
  private CatalogPriceType priceType;

  @Column(name = "custom_name", length = 80)
  private String customName;

  @Enumerated(EnumType.STRING)
  @Column(name = "base_mode", nullable = false, length = 20)
  private PriceBaseMode baseMode = PriceBaseMode.NONE;

  @Enumerated(EnumType.STRING)
  @Column(name = "base_price_type", length = 20)
  private CatalogPriceType basePriceType;

  @Enumerated(EnumType.STRING)
  @Column(name = "adjustment_kind_default", nullable = false, length = 20)
  private PriceAdjustmentKind adjustmentKindDefault = PriceAdjustmentKind.FIXED;

  @Column(name = "adjustment_default", nullable = false, precision = 19, scale = 6)
  private BigDecimal adjustmentDefault = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(name = "ui_lock_mode", nullable = false, length = 10)
  private PriceUiLockMode uiLockMode = PriceUiLockMode.II;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getCatalogConfigurationByGroupId() {
    return catalogConfigurationByGroupId;
  }

  public void setCatalogConfigurationByGroupId(Long catalogConfigurationByGroupId) {
    this.catalogConfigurationByGroupId = catalogConfigurationByGroupId;
  }

  public CatalogPriceType getPriceType() {
    return priceType;
  }

  public void setPriceType(CatalogPriceType priceType) {
    this.priceType = priceType;
  }

  public String getCustomName() {
    return customName;
  }

  public void setCustomName(String customName) {
    this.customName = customName;
  }

  public PriceBaseMode getBaseMode() {
    return baseMode;
  }

  public void setBaseMode(PriceBaseMode baseMode) {
    this.baseMode = baseMode;
  }

  public CatalogPriceType getBasePriceType() {
    return basePriceType;
  }

  public void setBasePriceType(CatalogPriceType basePriceType) {
    this.basePriceType = basePriceType;
  }

  public PriceAdjustmentKind getAdjustmentKindDefault() {
    return adjustmentKindDefault;
  }

  public void setAdjustmentKindDefault(PriceAdjustmentKind adjustmentKindDefault) {
    this.adjustmentKindDefault = adjustmentKindDefault;
  }

  public BigDecimal getAdjustmentDefault() {
    return adjustmentDefault;
  }

  public void setAdjustmentDefault(BigDecimal adjustmentDefault) {
    this.adjustmentDefault = adjustmentDefault;
  }

  public PriceUiLockMode getUiLockMode() {
    return uiLockMode;
  }

  public void setUiLockMode(PriceUiLockMode uiLockMode) {
    this.uiLockMode = uiLockMode;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
