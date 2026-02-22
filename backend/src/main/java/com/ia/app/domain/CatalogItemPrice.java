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
  name = "catalog_item_price",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_catalog_item_price_scope",
      columnNames = {"tenant_id", "catalog_type", "catalog_item_id", "price_type"})
  })
public class CatalogItemPrice extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "catalog_type", nullable = false, length = 20)
  private CatalogConfigurationType catalogType;

  @Column(name = "catalog_item_id", nullable = false)
  private Long catalogItemId;

  @Enumerated(EnumType.STRING)
  @Column(name = "price_type", nullable = false, length = 20)
  private CatalogPriceType priceType;

  @Column(name = "price_final", nullable = false, precision = 19, scale = 6)
  private BigDecimal priceFinal = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(name = "adjustment_kind", nullable = false, length = 20)
  private PriceAdjustmentKind adjustmentKind = PriceAdjustmentKind.FIXED;

  @Column(name = "adjustment_value", nullable = false, precision = 19, scale = 6)
  private BigDecimal adjustmentValue = BigDecimal.ZERO;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public CatalogConfigurationType getCatalogType() {
    return catalogType;
  }

  public void setCatalogType(CatalogConfigurationType catalogType) {
    this.catalogType = catalogType;
  }

  public Long getCatalogItemId() {
    return catalogItemId;
  }

  public void setCatalogItemId(Long catalogItemId) {
    this.catalogItemId = catalogItemId;
  }

  public CatalogPriceType getPriceType() {
    return priceType;
  }

  public void setPriceType(CatalogPriceType priceType) {
    this.priceType = priceType;
  }

  public BigDecimal getPriceFinal() {
    return priceFinal;
  }

  public void setPriceFinal(BigDecimal priceFinal) {
    this.priceFinal = priceFinal;
  }

  public PriceAdjustmentKind getAdjustmentKind() {
    return adjustmentKind;
  }

  public void setAdjustmentKind(PriceAdjustmentKind adjustmentKind) {
    this.adjustmentKind = adjustmentKind;
  }

  public BigDecimal getAdjustmentValue() {
    return adjustmentValue;
  }

  public void setAdjustmentValue(BigDecimal adjustmentValue) {
    this.adjustmentValue = adjustmentValue;
  }
}
