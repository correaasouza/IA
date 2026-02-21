package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.util.UUID;

@MappedSuperclass
public abstract class CatalogItemBase extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "catalog_configuration_id", nullable = false)
  private Long catalogConfigurationId;

  @Column(name = "agrupador_empresa_id", nullable = false)
  private Long agrupadorEmpresaId;

  @Column(name = "catalog_group_id")
  private Long catalogGroupId;

  @Column(name = "codigo", nullable = false)
  private Long codigo;

  @Column(name = "nome", nullable = false, length = 200)
  private String nome;

  @Column(name = "descricao", length = 255)
  private String descricao;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  @Column(name = "tenant_unit_id")
  private UUID tenantUnitId;

  @Column(name = "unidade_alternativa_tenant_unit_id")
  private UUID unidadeAlternativaTenantUnitId;

  @Column(name = "fator_conversao_alternativa", precision = 24, scale = 12)
  private BigDecimal fatorConversaoAlternativa;

  @Column(name = "has_stock_movements", nullable = false)
  private boolean hasStockMovements = false;

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

  public Long getCatalogGroupId() {
    return catalogGroupId;
  }

  public void setCatalogGroupId(Long catalogGroupId) {
    this.catalogGroupId = catalogGroupId;
  }

  public Long getCodigo() {
    return codigo;
  }

  public void setCodigo(Long codigo) {
    this.codigo = codigo;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String descricao) {
    this.descricao = descricao;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }

  public UUID getTenantUnitId() {
    return tenantUnitId;
  }

  public void setTenantUnitId(UUID tenantUnitId) {
    this.tenantUnitId = tenantUnitId;
  }

  public UUID getUnidadeAlternativaTenantUnitId() {
    return unidadeAlternativaTenantUnitId;
  }

  public void setUnidadeAlternativaTenantUnitId(UUID unidadeAlternativaTenantUnitId) {
    this.unidadeAlternativaTenantUnitId = unidadeAlternativaTenantUnitId;
  }

  public BigDecimal getFatorConversaoAlternativa() {
    return fatorConversaoAlternativa;
  }

  public void setFatorConversaoAlternativa(BigDecimal fatorConversaoAlternativa) {
    this.fatorConversaoAlternativa = fatorConversaoAlternativa;
  }

  public boolean isHasStockMovements() {
    return hasStockMovements;
  }

  public void setHasStockMovements(boolean hasStockMovements) {
    this.hasStockMovements = hasStockMovements;
  }
}
