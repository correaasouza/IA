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
  name = "catalog_stock_adjustment",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_catalog_stock_adjustment_tenant_codigo",
      columnNames = {"tenant_id", "codigo"})
  })
public class CatalogStockAdjustment extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "catalog_configuration_id", nullable = false)
  private Long catalogConfigurationId;

  @Column(name = "codigo", nullable = false, length = 40)
  private String codigo;

  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  @Column(name = "tipo", nullable = false, length = 20)
  private String tipo = CatalogStockAdjustmentType.ENTRADA.name();

  @Column(name = "ordem", nullable = false)
  private Integer ordem = 1;

  @Column(name = "estoque_origem_agrupador_id")
  private Long estoqueOrigemAgrupadorId;

  @Column(name = "estoque_origem_tipo_id")
  private Long estoqueOrigemTipoId;

  @Column(name = "estoque_origem_filial_id")
  private Long estoqueOrigemFilialId;

  @Column(name = "estoque_destino_agrupador_id")
  private Long estoqueDestinoAgrupadorId;

  @Column(name = "estoque_destino_tipo_id")
  private Long estoqueDestinoTipoId;

  @Column(name = "estoque_destino_filial_id")
  private Long estoqueDestinoFilialId;

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

  public String getTipo() {
    return tipo;
  }

  public void setTipo(String tipo) {
    this.tipo = tipo;
  }

  public Integer getOrdem() {
    return ordem;
  }

  public void setOrdem(Integer ordem) {
    this.ordem = ordem;
  }

  public Long getEstoqueOrigemAgrupadorId() {
    return estoqueOrigemAgrupadorId;
  }

  public void setEstoqueOrigemAgrupadorId(Long estoqueOrigemAgrupadorId) {
    this.estoqueOrigemAgrupadorId = estoqueOrigemAgrupadorId;
  }

  public Long getEstoqueOrigemTipoId() {
    return estoqueOrigemTipoId;
  }

  public void setEstoqueOrigemTipoId(Long estoqueOrigemTipoId) {
    this.estoqueOrigemTipoId = estoqueOrigemTipoId;
  }

  public Long getEstoqueOrigemFilialId() {
    return estoqueOrigemFilialId;
  }

  public void setEstoqueOrigemFilialId(Long estoqueOrigemFilialId) {
    this.estoqueOrigemFilialId = estoqueOrigemFilialId;
  }

  public Long getEstoqueDestinoAgrupadorId() {
    return estoqueDestinoAgrupadorId;
  }

  public void setEstoqueDestinoAgrupadorId(Long estoqueDestinoAgrupadorId) {
    this.estoqueDestinoAgrupadorId = estoqueDestinoAgrupadorId;
  }

  public Long getEstoqueDestinoTipoId() {
    return estoqueDestinoTipoId;
  }

  public void setEstoqueDestinoTipoId(Long estoqueDestinoTipoId) {
    this.estoqueDestinoTipoId = estoqueDestinoTipoId;
  }

  public Long getEstoqueDestinoFilialId() {
    return estoqueDestinoFilialId;
  }

  public void setEstoqueDestinoFilialId(Long estoqueDestinoFilialId) {
    this.estoqueDestinoFilialId = estoqueDestinoFilialId;
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
