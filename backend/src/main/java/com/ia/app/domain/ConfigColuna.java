package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "config_coluna")
public class ConfigColuna extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "screen_id", nullable = false, length = 120)
  private String screenId;

  @Column(name = "scope_tipo", nullable = false, length = 20)
  private String scopeTipo;

  @Column(name = "scope_valor", length = 120)
  private String scopeValor;

  @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
  private String configJson;

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

  public String getScreenId() {
    return screenId;
  }

  public void setScreenId(String screenId) {
    this.screenId = screenId;
  }

  public String getScopeTipo() {
    return scopeTipo;
  }

  public void setScopeTipo(String scopeTipo) {
    this.scopeTipo = scopeTipo;
  }

  public String getScopeValor() {
    return scopeValor;
  }

  public void setScopeValor(String scopeValor) {
    this.scopeValor = scopeValor;
  }

  public String getConfigJson() {
    return configJson;
  }

  public void setConfigJson(String configJson) {
    this.configJson = configJson;
  }

  public Integer getVersao() {
    return versao;
  }

  public void setVersao(Integer versao) {
    this.versao = versao;
  }
}
