package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
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

  @Column(name = "empresa_id", nullable = false)
  private Long empresaId;

  @Column(name = "codigo", nullable = false)
  private Long codigo;

  @Column(name = "pessoa_id", nullable = false)
  private Long pessoaId;

  @Column(name = "grupo_entidade_id")
  private Long grupoEntidadeId;

  @Column(name = "price_book_id")
  private Long priceBookId;

  @Column(name = "alerta", length = 1000)
  private String alerta;

  @Column(name = "observacao")
  private String observacao;

  @Column(name = "parecer")
  private String parecer;

  @Column(name = "codigo_barras", length = 60)
  private String codigoBarras;

  @Column(name = "texto_termo_quitacao", length = 4096)
  private String textoTermoQuitacao;

  @Column(name = "tratamento_id")
  private Long tratamentoId;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

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

  public Long getTipoEntidadeConfigAgrupadorId() {
    return tipoEntidadeConfigAgrupadorId;
  }

  public void setTipoEntidadeConfigAgrupadorId(Long tipoEntidadeConfigAgrupadorId) {
    this.tipoEntidadeConfigAgrupadorId = tipoEntidadeConfigAgrupadorId;
  }

  public Long getEmpresaId() {
    return empresaId;
  }

  public void setEmpresaId(Long empresaId) {
    this.empresaId = empresaId;
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

  public String getAlerta() {
    return alerta;
  }

  public void setAlerta(String alerta) {
    this.alerta = alerta;
  }

  public String getObservacao() {
    return observacao;
  }

  public void setObservacao(String observacao) {
    this.observacao = observacao;
  }

  public String getParecer() {
    return parecer;
  }

  public void setParecer(String parecer) {
    this.parecer = parecer;
  }

  public String getCodigoBarras() {
    return codigoBarras;
  }

  public void setCodigoBarras(String codigoBarras) {
    this.codigoBarras = codigoBarras;
  }

  public String getTextoTermoQuitacao() {
    return textoTermoQuitacao;
  }

  public void setTextoTermoQuitacao(String textoTermoQuitacao) {
    this.textoTermoQuitacao = textoTermoQuitacao;
  }

  public Long getTratamentoId() {
    return tratamentoId;
  }

  public void setTratamentoId(Long tratamentoId) {
    this.tratamentoId = tratamentoId;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
