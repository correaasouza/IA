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
  name = "catalog_stock_balance",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_catalog_stock_balance_scope",
      columnNames = {"tenant_id", "catalog_type", "catalogo_id", "agrupador_empresa_id", "estoque_tipo_id", "filial_id"})
  })
public class CatalogStockBalance extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "catalogo_id", nullable = false)
  private Long catalogoId;

  @Enumerated(EnumType.STRING)
  @Column(name = "catalog_type", nullable = false, length = 20)
  private CatalogConfigurationType catalogType;

  @Column(name = "catalog_configuration_id", nullable = false)
  private Long catalogConfigurationId;

  @Column(name = "agrupador_empresa_id", nullable = false)
  private Long agrupadorEmpresaId;

  @Column(name = "estoque_tipo_id", nullable = false)
  private Long estoqueTipoId;

  @Column(name = "filial_id", nullable = false)
  private Long filialId;

  @Column(name = "quantidade_atual", nullable = false, precision = 19, scale = 6)
  private java.math.BigDecimal quantidadeAtual = java.math.BigDecimal.ZERO;

  @Column(name = "preco_atual", nullable = false, precision = 19, scale = 6)
  private java.math.BigDecimal precoAtual = java.math.BigDecimal.ZERO;

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

  public Long getCatalogoId() {
    return catalogoId;
  }

  public void setCatalogoId(Long catalogoId) {
    this.catalogoId = catalogoId;
  }

  public CatalogConfigurationType getCatalogType() {
    return catalogType;
  }

  public void setCatalogType(CatalogConfigurationType catalogType) {
    this.catalogType = catalogType;
  }

  public Long getCatalogConfigurationId() {
    return catalogConfigurationId;
  }

  public void setCatalogConfigurationId(Long catalogConfigurationId) {
    this.catalogConfigurationId = catalogConfigurationId;
  }

  public Long getAgrupadorEmpresaId() {
    return agrupadorEmpresaId;
  }

  public void setAgrupadorEmpresaId(Long agrupadorEmpresaId) {
    this.agrupadorEmpresaId = agrupadorEmpresaId;
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

  public java.math.BigDecimal getQuantidadeAtual() {
    return quantidadeAtual;
  }

  public void setQuantidadeAtual(java.math.BigDecimal quantidadeAtual) {
    this.quantidadeAtual = quantidadeAtual;
  }

  public java.math.BigDecimal getPrecoAtual() {
    return precoAtual;
  }

  public void setPrecoAtual(java.math.BigDecimal precoAtual) {
    this.precoAtual = precoAtual;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
