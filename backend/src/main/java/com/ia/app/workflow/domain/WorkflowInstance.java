package com.ia.app.workflow.domain;

import com.ia.app.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
  name = "workflow_instance",
  uniqueConstraints = {
    @UniqueConstraint(name = "ux_workflow_instance_origin_entity", columnNames = {"tenant_id", "origin", "entity_id"})
  })
public class WorkflowInstance extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "origin", nullable = false, length = 60)
  private WorkflowOrigin origin;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "definition_id", nullable = false)
  private WorkflowDefinition definition;

  @Column(name = "definition_version", nullable = false)
  private Integer definitionVersion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_state_id", nullable = false)
  private WorkflowState currentState;

  @Column(name = "current_state_key", nullable = false, length = 80)
  private String currentStateKey;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "last_transition_id")
  private WorkflowTransition lastTransition;

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

  public WorkflowOrigin getOrigin() {
    return origin;
  }

  public void setOrigin(WorkflowOrigin origin) {
    this.origin = origin;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public WorkflowDefinition getDefinition() {
    return definition;
  }

  public void setDefinition(WorkflowDefinition definition) {
    this.definition = definition;
  }

  public Integer getDefinitionVersion() {
    return definitionVersion;
  }

  public void setDefinitionVersion(Integer definitionVersion) {
    this.definitionVersion = definitionVersion;
  }

  public WorkflowState getCurrentState() {
    return currentState;
  }

  public void setCurrentState(WorkflowState currentState) {
    this.currentState = currentState;
  }

  public String getCurrentStateKey() {
    return currentStateKey;
  }

  public void setCurrentStateKey(String currentStateKey) {
    this.currentStateKey = currentStateKey;
  }

  public WorkflowTransition getLastTransition() {
    return lastTransition;
  }

  public void setLastTransition(WorkflowTransition lastTransition) {
    this.lastTransition = lastTransition;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
