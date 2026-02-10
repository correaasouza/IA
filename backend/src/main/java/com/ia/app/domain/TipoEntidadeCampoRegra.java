package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_entidade_campo_regra")
public class TipoEntidadeCampoRegra extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "tipo_entidade_id", nullable = false)
  private Long tipoEntidadeId;

  @Column(name = "campo", nullable = false, length = 60)
  private String campo;

  @Column(name = "habilitado", nullable = false)
  private boolean habilitado = true;

  @Column(name = "requerido", nullable = false)
  private boolean requerido = false;

  @Column(name = "visivel", nullable = false)
  private boolean visivel = true;

  @Column(name = "editavel", nullable = false)
  private boolean editavel = true;

  @Column(name = "label", length = 120)
  private String label;

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

  public String getCampo() {
    return campo;
  }

  public void setCampo(String campo) {
    this.campo = campo;
  }

  public boolean isHabilitado() {
    return habilitado;
  }

  public void setHabilitado(boolean habilitado) {
    this.habilitado = habilitado;
  }

  public boolean isRequerido() {
    return requerido;
  }

  public void setRequerido(boolean requerido) {
    this.requerido = requerido;
  }

  public boolean isVisivel() {
    return visivel;
  }

  public void setVisivel(boolean visivel) {
    this.visivel = visivel;
  }

  public boolean isEditavel() {
    return editavel;
  }

  public void setEditavel(boolean editavel) {
    this.editavel = editavel;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Integer getVersao() {
    return versao;
  }

  public void setVersao(Integer versao) {
    this.versao = versao;
  }
}
