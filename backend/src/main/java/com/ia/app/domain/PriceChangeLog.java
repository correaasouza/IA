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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_change_log")
public class PriceChangeLog extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "sale_price_id")
  private Long salePriceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 20)
  private PriceChangeAction action;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_type", length = 40)
  private PriceChangeSourceType sourceType;

  @Enumerated(EnumType.STRING)
  @Column(name = "origin_type", length = 40)
  private PriceChangeOriginType originType;

  @Column(name = "origin_id")
  private Long originId;

  @Column(name = "old_price_final", precision = 19, scale = 6)
  private BigDecimal oldPriceFinal;

  @Column(name = "new_price_final", precision = 19, scale = 6)
  private BigDecimal newPriceFinal;

  @Column(name = "price_book_id")
  private Long priceBookId;

  @Column(name = "price_book_name", length = 120)
  private String priceBookName;

  @Column(name = "variant_id")
  private Long variantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "price_type", length = 20)
  private CatalogPriceType priceType;

  @Enumerated(EnumType.STRING)
  @Column(name = "catalog_type", nullable = false, length = 20)
  private CatalogConfigurationType catalogType;

  @Column(name = "catalog_item_id", nullable = false)
  private Long catalogItemId;

  @Column(name = "tenant_unit_id")
  private UUID tenantUnitId;

  @Column(name = "changed_by", length = 120)
  private String changedBy;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getSalePriceId() {
    return salePriceId;
  }

  public void setSalePriceId(Long salePriceId) {
    this.salePriceId = salePriceId;
  }

  public PriceChangeAction getAction() {
    return action;
  }

  public void setAction(PriceChangeAction action) {
    this.action = action;
  }

  public PriceChangeSourceType getSourceType() {
    return sourceType;
  }

  public void setSourceType(PriceChangeSourceType sourceType) {
    this.sourceType = sourceType;
  }

  public PriceChangeOriginType getOriginType() {
    return originType;
  }

  public void setOriginType(PriceChangeOriginType originType) {
    this.originType = originType;
  }

  public Long getOriginId() {
    return originId;
  }

  public void setOriginId(Long originId) {
    this.originId = originId;
  }

  public BigDecimal getOldPriceFinal() {
    return oldPriceFinal;
  }

  public void setOldPriceFinal(BigDecimal oldPriceFinal) {
    this.oldPriceFinal = oldPriceFinal;
  }

  public BigDecimal getNewPriceFinal() {
    return newPriceFinal;
  }

  public void setNewPriceFinal(BigDecimal newPriceFinal) {
    this.newPriceFinal = newPriceFinal;
  }

  public Long getPriceBookId() {
    return priceBookId;
  }

  public void setPriceBookId(Long priceBookId) {
    this.priceBookId = priceBookId;
  }

  public String getPriceBookName() {
    return priceBookName;
  }

  public void setPriceBookName(String priceBookName) {
    this.priceBookName = priceBookName;
  }

  public Long getVariantId() {
    return variantId;
  }

  public void setVariantId(Long variantId) {
    this.variantId = variantId;
  }

  public CatalogPriceType getPriceType() {
    return priceType;
  }

  public void setPriceType(CatalogPriceType priceType) {
    this.priceType = priceType;
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

  public String getChangedBy() {
    return changedBy;
  }

  public void setChangedBy(String changedBy) {
    this.changedBy = changedBy;
  }

  public Instant getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(Instant changedAt) {
    this.changedAt = changedAt;
  }
}
