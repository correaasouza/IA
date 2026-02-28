package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "entidade_form_field_config")
public class EntidadeFormFieldConfig extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "group_config_id", nullable = false)
  private Long groupConfigId;

  @Column(name = "field_key", nullable = false, length = 120)
  private String fieldKey;

  @Column(name = "label", length = 160)
  private String label;

  @Column(name = "ordem", nullable = false)
  private Integer ordem = 0;

  @Column(name = "visible", nullable = false)
  private boolean visible = true;

  @Column(name = "editable", nullable = false)
  private boolean editable = true;

  @Column(name = "required", nullable = false)
  private boolean required = false;

  @Column(name = "default_value_json")
  private String defaultValueJson;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getGroupConfigId() {
    return groupConfigId;
  }

  public void setGroupConfigId(Long groupConfigId) {
    this.groupConfigId = groupConfigId;
  }

  public String getFieldKey() {
    return fieldKey;
  }

  public void setFieldKey(String fieldKey) {
    this.fieldKey = fieldKey;
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

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public boolean isEditable() {
    return editable;
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public String getDefaultValueJson() {
    return defaultValueJson;
  }

  public void setDefaultValueJson(String defaultValueJson) {
    this.defaultValueJson = defaultValueJson;
  }
}

