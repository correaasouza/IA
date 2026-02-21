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
import java.util.UUID;

@Entity
@Table(
  name = "catalog_movement",
  uniqueConstraints = {
    @UniqueConstraint(name = "ux_catalog_movement_idempotency", columnNames = {"tenant_id", "idempotency_key"})
  })
public class CatalogMovement extends AuditableEntity {

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

  @Enumerated(EnumType.STRING)
  @Column(name = "origem_movimentacao_tipo", nullable = false, length = 40)
  private CatalogMovementOriginType origemMovimentacaoTipo;

  @Column(name = "origem_movimentacao_codigo", length = 120)
  private String origemMovimentacaoCodigo;

  @Column(name = "origem_movimento_item_codigo", length = 120)
  private String origemMovimentoItemCodigo;

  @Column(name = "data_hora_movimentacao", nullable = false)
  private java.time.Instant dataHoraMovimentacao;

  @Column(name = "observacao", length = 255)
  private String observacao;

  @Column(name = "idempotency_key", nullable = false, length = 180)
  private String idempotencyKey;

  @Column(name = "tenant_unit_id")
  private UUID tenantUnitId;

  @Column(name = "unidade_base_catalogo_tenant_unit_id")
  private UUID unidadeBaseCatalogoTenantUnitId;

  @Column(name = "quantidade_informada", precision = 19, scale = 6)
  private BigDecimal quantidadeInformada;

  @Column(name = "quantidade_convertida_base", precision = 19, scale = 6)
  private BigDecimal quantidadeConvertidaBase;

  @Column(name = "fator_aplicado", precision = 24, scale = 12)
  private BigDecimal fatorAplicado;

  @Enumerated(EnumType.STRING)
  @Column(name = "fator_fonte", length = 40)
  private ConversionFactorSource fatorFonte;

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

  public CatalogMovementOriginType getOrigemMovimentacaoTipo() {
    return origemMovimentacaoTipo;
  }

  public void setOrigemMovimentacaoTipo(CatalogMovementOriginType origemMovimentacaoTipo) {
    this.origemMovimentacaoTipo = origemMovimentacaoTipo;
  }

  public String getOrigemMovimentacaoCodigo() {
    return origemMovimentacaoCodigo;
  }

  public void setOrigemMovimentacaoCodigo(String origemMovimentacaoCodigo) {
    this.origemMovimentacaoCodigo = origemMovimentacaoCodigo;
  }

  public String getOrigemMovimentoItemCodigo() {
    return origemMovimentoItemCodigo;
  }

  public void setOrigemMovimentoItemCodigo(String origemMovimentoItemCodigo) {
    this.origemMovimentoItemCodigo = origemMovimentoItemCodigo;
  }

  public java.time.Instant getDataHoraMovimentacao() {
    return dataHoraMovimentacao;
  }

  public void setDataHoraMovimentacao(java.time.Instant dataHoraMovimentacao) {
    this.dataHoraMovimentacao = dataHoraMovimentacao;
  }

  public String getObservacao() {
    return observacao;
  }

  public void setObservacao(String observacao) {
    this.observacao = observacao;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public UUID getTenantUnitId() {
    return tenantUnitId;
  }

  public void setTenantUnitId(UUID tenantUnitId) {
    this.tenantUnitId = tenantUnitId;
  }

  public UUID getUnidadeBaseCatalogoTenantUnitId() {
    return unidadeBaseCatalogoTenantUnitId;
  }

  public void setUnidadeBaseCatalogoTenantUnitId(UUID unidadeBaseCatalogoTenantUnitId) {
    this.unidadeBaseCatalogoTenantUnitId = unidadeBaseCatalogoTenantUnitId;
  }

  public BigDecimal getQuantidadeInformada() {
    return quantidadeInformada;
  }

  public void setQuantidadeInformada(BigDecimal quantidadeInformada) {
    this.quantidadeInformada = quantidadeInformada;
  }

  public BigDecimal getQuantidadeConvertidaBase() {
    return quantidadeConvertidaBase;
  }

  public void setQuantidadeConvertidaBase(BigDecimal quantidadeConvertidaBase) {
    this.quantidadeConvertidaBase = quantidadeConvertidaBase;
  }

  public BigDecimal getFatorAplicado() {
    return fatorAplicado;
  }

  public void setFatorAplicado(BigDecimal fatorAplicado) {
    this.fatorAplicado = fatorAplicado;
  }

  public ConversionFactorSource getFatorFonte() {
    return fatorFonte;
  }

  public void setFatorFonte(ConversionFactorSource fatorFonte) {
    this.fatorFonte = fatorFonte;
  }
}
