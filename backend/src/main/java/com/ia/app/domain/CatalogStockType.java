package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
  name = "catalog_stock_type",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_catalog_stock_type_scope_codigo_active",
      columnNames = {"tenant_id", "catalog_configuration_id", "agrupador_empresa_id", "codigo", "active"})
  })
public class CatalogStockType extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "catalog_configuration_id", nullable = false)
  private Long catalogConfigurationId;

  @Column(name = "agrupador_empresa_id", nullable = false)
  private Long agrupadorEmpresaId;

  @Column(name = "codigo", nullable = false, length = 40)
  private String codigo;

  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  @Column(name = "ordem", nullable = false)
  private Integer ordem = 1;

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

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigo) {
    this.codigo = codigo;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public Integer getOrdem() {
    return ordem;
  }

  public void setOrdem(Integer ordem) {
    this.ordem = ordem;
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
