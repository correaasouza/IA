package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
  name = "registro_entidade",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_registro_entidade_codigo_scope",
      columnNames = {"tenant_id", "tipo_entidade_config_agrupador_id", "codigo"})
  })
public class RegistroEntidade extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "tipo_entidade_config_agrupador_id", nullable = false)
  private Long tipoEntidadeConfigAgrupadorId;

  @Column(name = "codigo", nullable = false)
  private Long codigo;

  @Column(name = "pessoa_id", nullable = false)
  private Long pessoaId;

  @Column(name = "grupo_entidade_id")
  private Long grupoEntidadeId;

  @Column(name = "price_book_id")
  private Long priceBookId;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getTipoEntidadeConfigAgrupadorId() {
    return tipoEntidadeConfigAgrupadorId;
  }

  public void setTipoEntidadeConfigAgrupadorId(Long tipoEntidadeConfigAgrupadorId) {
    this.tipoEntidadeConfigAgrupadorId = tipoEntidadeConfigAgrupadorId;
  }

  public Long getCodigo() {
    return codigo;
  }

  public void setCodigo(Long codigo) {
    this.codigo = codigo;
  }

  public Long getPessoaId() {
    return pessoaId;
  }

  public void setPessoaId(Long pessoaId) {
    this.pessoaId = pessoaId;
  }

  public Long getGrupoEntidadeId() {
    return grupoEntidadeId;
  }

  public void setGrupoEntidadeId(Long grupoEntidadeId) {
    this.grupoEntidadeId = grupoEntidadeId;
  }

  public Long getPriceBookId() {
    return priceBookId;
  }

  public void setPriceBookId(Long priceBookId) {
    this.priceBookId = priceBookId;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }
}
