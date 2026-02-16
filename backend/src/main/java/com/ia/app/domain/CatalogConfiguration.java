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
import jakarta.persistence.Version;

@Entity
@Table(
  name = "catalog_configuration",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_catalog_configuration_tenant_type",
      columnNames = {"tenant_id", "type"})
  })
public class CatalogConfiguration extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private CatalogConfigurationType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "numbering_mode", nullable = false, length = 20)
  private CatalogNumberingMode numberingMode = CatalogNumberingMode.AUTOMATICA;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public CatalogConfigurationType getType() {
    return type;
  }

  public void setType(CatalogConfigurationType type) {
    this.type = type;
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

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
