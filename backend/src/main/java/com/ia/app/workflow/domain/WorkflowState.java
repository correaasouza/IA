package com.ia.app.workflow.domain;

import com.ia.app.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
  name = "workflow_state",
  uniqueConstraints = {
    @UniqueConstraint(name = "ux_workflow_state_def_key", columnNames = {"definition_id", "state_key"})
  })
public class WorkflowState extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "definition_id", nullable = false)
  private WorkflowDefinition definition;

  @Column(name = "state_key", nullable = false, length = 80)
  private String stateKey;

  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Column(name = "color", length = 20)
  private String color;

  @Column(name = "is_initial", nullable = false)
  private boolean initial;

  @Column(name = "is_final", nullable = false)
  private boolean terminal;

  @Column(name = "ui_x", nullable = false)
  private Integer uiX = 0;

  @Column(name = "ui_y", nullable = false)
  private Integer uiY = 0;

  @Column(name = "metadata_json", columnDefinition = "TEXT")
  private String metadataJson;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public WorkflowDefinition getDefinition() {
    return definition;
  }

  public void setDefinition(WorkflowDefinition definition) {
    this.definition = definition;
  }

  public String getStateKey() {
    return stateKey;
  }

  public void setStateKey(String stateKey) {
    this.stateKey = stateKey;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public boolean isInitial() {
    return initial;
  }

  public void setInitial(boolean initial) {
    this.initial = initial;
  }

  public boolean isTerminal() {
    return terminal;
  }

  public void setTerminal(boolean terminal) {
    this.terminal = terminal;
  }

  public Integer getUiX() {
    return uiX;
  }

  public void setUiX(Integer uiX) {
    this.uiX = uiX;
  }

  public Integer getUiY() {
    return uiY;
  }

  public void setUiY(Integer uiY) {
    this.uiY = uiY;
  }

  public String getMetadataJson() {
    return metadataJson;
  }

  public void setMetadataJson(String metadataJson) {
    this.metadataJson = metadataJson;
  }
}
