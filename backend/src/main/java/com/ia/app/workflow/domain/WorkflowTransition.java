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
  name = "workflow_transition",
  uniqueConstraints = {
    @UniqueConstraint(name = "ux_workflow_transition_def_key", columnNames = {"definition_id", "transition_key"})
  })
public class WorkflowTransition extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "definition_id", nullable = false)
  private WorkflowDefinition definition;

  @Column(name = "transition_key", nullable = false, length = 80)
  private String transitionKey;

  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_state_id", nullable = false)
  private WorkflowState fromState;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "to_state_id", nullable = false)
  private WorkflowState toState;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "priority", nullable = false)
  private Integer priority = 100;

  @Column(name = "actions_json", columnDefinition = "TEXT")
  private String actionsJson;

  @Column(name = "ui_meta_json", columnDefinition = "TEXT")
  private String uiMetaJson;

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

  public String getTransitionKey() {
    return transitionKey;
  }

  public void setTransitionKey(String transitionKey) {
    this.transitionKey = transitionKey;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public WorkflowState getFromState() {
    return fromState;
  }

  public void setFromState(WorkflowState fromState) {
    this.fromState = fromState;
  }

  public WorkflowState getToState() {
    return toState;
  }

  public void setToState(WorkflowState toState) {
    this.toState = toState;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public String getActionsJson() {
    return actionsJson;
  }

  public void setActionsJson(String actionsJson) {
    this.actionsJson = actionsJson;
  }

  public String getUiMetaJson() {
    return uiMetaJson;
  }

  public void setUiMetaJson(String uiMetaJson) {
    this.uiMetaJson = uiMetaJson;
  }
}
