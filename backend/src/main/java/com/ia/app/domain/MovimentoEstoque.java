package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
  name = "movimento_estoque",
  uniqueConstraints = {
    @UniqueConstraint(name = "ux_movimento_estoque_id_tenant", columnNames = {"id", "tenant_id"})
  })
public class MovimentoEstoque extends MovimentoBase {

  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  @Column(name = "movimento_config_id", nullable = false)
  private Long movimentoConfigId;

  @Column(name = "tipo_entidade_padrao_id")
  private Long tipoEntidadePadraoId;

  @Column(name = "stock_adjustment_id")
  private Long stockAdjustmentId;

  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  public MovimentoEstoque() {
    setTipoMovimento(MovimentoTipo.MOVIMENTO_ESTOQUE);
  }

  @PrePersist
  @PreUpdate
  void ensureTipo() {
    setTipoMovimento(MovimentoTipo.MOVIMENTO_ESTOQUE);
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public Long getMovimentoConfigId() {
    return movimentoConfigId;
  }

  public void setMovimentoConfigId(Long movimentoConfigId) {
    this.movimentoConfigId = movimentoConfigId;
  }

  public Long getTipoEntidadePadraoId() {
    return tipoEntidadePadraoId;
  }

  public void setTipoEntidadePadraoId(Long tipoEntidadePadraoId) {
    this.tipoEntidadePadraoId = tipoEntidadePadraoId;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public Long getStockAdjustmentId() {
    return stockAdjustmentId;
  }

  public void setStockAdjustmentId(Long stockAdjustmentId) {
    this.stockAdjustmentId = stockAdjustmentId;
  }
}
