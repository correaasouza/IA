package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "movimento_estoque_item")
public class MovimentoEstoqueItem extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "movimento_estoque_id", nullable = false)
  private Long movimentoEstoqueId;

  @Column(name = "movimento_item_tipo_id", nullable = false)
  private Long movimentoItemTipoId;

  @Enumerated(EnumType.STRING)
  @Column(name = "catalog_type", nullable = false, length = 20)
  private CatalogConfigurationType catalogType;

  @Column(name = "catalog_item_id", nullable = false)
  private Long catalogItemId;

  @Column(name = "catalog_codigo_snapshot", nullable = false)
  private Long catalogCodigoSnapshot;

  @Column(name = "catalog_nome_snapshot", nullable = false, length = 200)
  private String catalogNomeSnapshot;

  @Column(name = "quantidade", nullable = false, precision = 19, scale = 6)
  private java.math.BigDecimal quantidade = java.math.BigDecimal.ZERO;

  @Column(name = "valor_unitario", nullable = false, precision = 19, scale = 6)
  private java.math.BigDecimal valorUnitario = java.math.BigDecimal.ZERO;

  @Column(name = "valor_total", nullable = false, precision = 19, scale = 6)
  private java.math.BigDecimal valorTotal = java.math.BigDecimal.ZERO;

  @Column(name = "cobrar", nullable = false)
  private boolean cobrar = true;

  @Column(name = "ordem", nullable = false)
  private Integer ordem = 0;

  @Column(name = "observacao", length = 255)
  private String observacao;

  @Column(name = "status", length = 80)
  private String status;

  @Column(name = "estoque_movimentado", nullable = false)
  private boolean estoqueMovimentado;

  @Column(name = "estoque_movimentado_em")
  private java.time.Instant estoqueMovimentadoEm;

  @Column(name = "estoque_movimentado_por", length = 120)
  private String estoqueMovimentadoPor;

  @Column(name = "estoque_movimentacao_id")
  private Long estoqueMovimentacaoId;

  @Column(name = "estoque_movimentacao_chave", length = 180)
  private String estoqueMovimentacaoChave;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getMovimentoEstoqueId() {
    return movimentoEstoqueId;
  }

  public void setMovimentoEstoqueId(Long movimentoEstoqueId) {
    this.movimentoEstoqueId = movimentoEstoqueId;
  }

  public Long getMovimentoItemTipoId() {
    return movimentoItemTipoId;
  }

  public void setMovimentoItemTipoId(Long movimentoItemTipoId) {
    this.movimentoItemTipoId = movimentoItemTipoId;
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

  public Long getCatalogCodigoSnapshot() {
    return catalogCodigoSnapshot;
  }

  public void setCatalogCodigoSnapshot(Long catalogCodigoSnapshot) {
    this.catalogCodigoSnapshot = catalogCodigoSnapshot;
  }

  public String getCatalogNomeSnapshot() {
    return catalogNomeSnapshot;
  }

  public void setCatalogNomeSnapshot(String catalogNomeSnapshot) {
    this.catalogNomeSnapshot = catalogNomeSnapshot;
  }

  public java.math.BigDecimal getQuantidade() {
    return quantidade;
  }

  public void setQuantidade(java.math.BigDecimal quantidade) {
    this.quantidade = quantidade;
  }

  public java.math.BigDecimal getValorUnitario() {
    return valorUnitario;
  }

  public void setValorUnitario(java.math.BigDecimal valorUnitario) {
    this.valorUnitario = valorUnitario;
  }

  public java.math.BigDecimal getValorTotal() {
    return valorTotal;
  }

  public void setValorTotal(java.math.BigDecimal valorTotal) {
    this.valorTotal = valorTotal;
  }

  public boolean isCobrar() {
    return cobrar;
  }

  public void setCobrar(boolean cobrar) {
    this.cobrar = cobrar;
  }

  public Integer getOrdem() {
    return ordem;
  }

  public void setOrdem(Integer ordem) {
    this.ordem = ordem;
  }

  public String getObservacao() {
    return observacao;
  }

  public void setObservacao(String observacao) {
    this.observacao = observacao;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public boolean isEstoqueMovimentado() {
    return estoqueMovimentado;
  }

  public void setEstoqueMovimentado(boolean estoqueMovimentado) {
    this.estoqueMovimentado = estoqueMovimentado;
  }

  public java.time.Instant getEstoqueMovimentadoEm() {
    return estoqueMovimentadoEm;
  }

  public void setEstoqueMovimentadoEm(java.time.Instant estoqueMovimentadoEm) {
    this.estoqueMovimentadoEm = estoqueMovimentadoEm;
  }

  public String getEstoqueMovimentadoPor() {
    return estoqueMovimentadoPor;
  }

  public void setEstoqueMovimentadoPor(String estoqueMovimentadoPor) {
    this.estoqueMovimentadoPor = estoqueMovimentadoPor;
  }

  public Long getEstoqueMovimentacaoId() {
    return estoqueMovimentacaoId;
  }

  public void setEstoqueMovimentacaoId(Long estoqueMovimentacaoId) {
    this.estoqueMovimentacaoId = estoqueMovimentacaoId;
  }

  public String getEstoqueMovimentacaoChave() {
    return estoqueMovimentacaoChave;
  }

  public void setEstoqueMovimentacaoChave(String estoqueMovimentacaoChave) {
    this.estoqueMovimentacaoChave = estoqueMovimentacaoChave;
  }
}
