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
import java.time.Instant;

@Entity
@Table(name = "workflow_history")
public class WorkflowHistory extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "instance_id", nullable = false)
  private WorkflowInstance instance;

  @Enumerated(EnumType.STRING)
  @Column(name = "origin", nullable = false, length = 60)
  private WorkflowOrigin origin;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  @Column(name = "from_state_key", nullable = false, length = 80)
  private String fromStateKey;

  @Column(name = "to_state_key", nullable = false, length = 80)
  private String toStateKey;

  @Column(name = "transition_key", nullable = false, length = 80)
  private String transitionKey;

  @Column(name = "triggered_by", nullable = false, length = 120)
  private String triggeredBy;

  @Column(name = "triggered_at", nullable = false)
  private Instant triggeredAt;

  @Column(name = "notes", length = 500)
  private String notes;

  @Column(name = "action_results_json", columnDefinition = "TEXT")
  private String actionResultsJson;

  @Column(name = "success", nullable = false)
  private boolean success = true;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public WorkflowInstance getInstance() {
    return instance;
  }

  public void setInstance(WorkflowInstance instance) {
    this.instance = instance;
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

  public String getFromStateKey() {
    return fromStateKey;
  }

  public void setFromStateKey(String fromStateKey) {
    this.fromStateKey = fromStateKey;
  }

  public String getToStateKey() {
    return toStateKey;
  }

  public void setToStateKey(String toStateKey) {
    this.toStateKey = toStateKey;
  }

  public String getTransitionKey() {
    return transitionKey;
  }

  public void setTransitionKey(String transitionKey) {
    this.transitionKey = transitionKey;
  }

  public String getTriggeredBy() {
    return triggeredBy;
  }

  public void setTriggeredBy(String triggeredBy) {
    this.triggeredBy = triggeredBy;
  }

  public Instant getTriggeredAt() {
    return triggeredAt;
  }

  public void setTriggeredAt(Instant triggeredAt) {
    this.triggeredAt = triggeredAt;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public String getActionResultsJson() {
    return actionResultsJson;
  }

  public void setActionResultsJson(String actionResultsJson) {
    this.actionResultsJson = actionResultsJson;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }
}
