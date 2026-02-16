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

@Entity
@Table(
  name = "catalog_configuration_by_group",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_catalog_cfg_group_active",
      columnNames = {"tenant_id", "catalog_configuration_id", "agrupador_id", "active"})
  })
public class CatalogConfigurationByGroup extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "catalog_configuration_id", nullable = false)
  private Long catalogConfigurationId;

  @Column(name = "agrupador_id", nullable = false)
  private Long agrupadorId;

  @Enumerated(EnumType.STRING)
  @Column(name = "numbering_mode", nullable = false, length = 20)
  private CatalogNumberingMode numberingMode = CatalogNumberingMode.AUTOMATICA;

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

  public Long getCatalogConfigurationId() {
    return catalogConfigurationId;
  }

  public void setCatalogConfigurationId(Long catalogConfigurationId) {
    this.catalogConfigurationId = catalogConfigurationId;
  }

  public Long getAgrupadorId() {
    return agrupadorId;
  }

  public void setAgrupadorId(Long agrupadorId) {
    this.agrupadorId = agrupadorId;
  }

  public CatalogNumberingMode getNumberingMode() {
    return numberingMode;
  }

  public void setNumberingMode(CatalogNumberingMode numberingMode) {
    this.numberingMode = numberingMode;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
