package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "campo_definicao")
public class CampoDefinicao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "tipo_entidade_id", nullable = false)
  private Long tipoEntidadeId;

  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  @Column(name = "label", length = 120)
  private String label;

  @Column(name = "tipo", nullable = false, length = 40)
  private String tipo;

  @Column(name = "obrigatorio", nullable = false)
  private boolean obrigatorio;

  @Column(name = "tamanho")
  private Integer tamanho;

  @Column(name = "versao", nullable = false)
  private Integer versao = 1;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getTipoEntidadeId() {
    return tipoEntidadeId;
  }

  public void setTipoEntidadeId(Long tipoEntidadeId) {
    this.tipoEntidadeId = tipoEntidadeId;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getTipo() {
    return tipo;
  }

  public void setTipo(String tipo) {
    this.tipo = tipo;
  }

  public boolean isObrigatorio() {
    return obrigatorio;
  }

  public void setObrigatorio(boolean obrigatorio) {
    this.obrigatorio = obrigatorio;
  }

  public Integer getTamanho() {
    return tamanho;
  }

  public void setTamanho(Integer tamanho) {
    this.tamanho = tamanho;
  }

  public Integer getVersao() {
    return versao;
  }

  public void setVersao(Integer versao) {
    this.versao = versao;
  }
}
