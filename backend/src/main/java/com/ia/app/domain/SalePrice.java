package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sale_price")
public class SalePrice extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "price_book_id", nullable = false)
  private Long priceBookId;

  @Column(name = "variant_id")
  private Long variantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "catalog_type", nullable = false, length = 20)
  private CatalogConfigurationType catalogType;

  @Column(name = "catalog_item_id", nullable = false)
  private Long catalogItemId;

  @Column(name = "tenant_unit_id")
  private UUID tenantUnitId;

  @Column(name = "price_final", nullable = false, precision = 19, scale = 6)
  private BigDecimal priceFinal = BigDecimal.ZERO;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getPriceBookId() {
    return priceBookId;
  }

  public void setPriceBookId(Long priceBookId) {
    this.priceBookId = priceBookId;
  }

  public Long getVariantId() {
    return variantId;
  }

  public void setVariantId(Long variantId) {
    this.variantId = variantId;
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

  public UUID getTenantUnitId() {
    return tenantUnitId;
  }

  public void setTenantUnitId(UUID tenantUnitId) {
    this.tenantUnitId = tenantUnitId;
  }

  public BigDecimal getPriceFinal() {
    return priceFinal;
  }

  public void setPriceFinal(BigDecimal priceFinal) {
    this.priceFinal = priceFinal;
  }
}
