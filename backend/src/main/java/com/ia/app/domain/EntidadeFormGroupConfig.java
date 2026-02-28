package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "entidade_form_group_config")
public class EntidadeFormGroupConfig extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "tipo_entidade_config_agrupador_id", nullable = false)
  private Long tipoEntidadeConfigAgrupadorId;

  @Column(name = "group_key", nullable = false, length = 80)
  private String groupKey;

  @Column(name = "label", length = 120)
  private String label;

  @Column(name = "ordem", nullable = false)
  private Integer ordem = 0;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "collapsed_by_default", nullable = false)
  private boolean collapsedByDefault = false;

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

  public String getGroupKey() {
    return groupKey;
  }

  public void setGroupKey(String groupKey) {
    this.groupKey = groupKey;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Integer getOrdem() {
    return ordem;
  }

  public void setOrdem(Integer ordem) {
    this.ordem = ordem;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isCollapsedByDefault() {
    return collapsedByDefault;
  }

  public void setCollapsedByDefault(boolean collapsedByDefault) {
    this.collapsedByDefault = collapsedByDefault;
  }
}

