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
import java.time.Instant;

@Entity
@Table(
  name = "workflow_action_execution",
  uniqueConstraints = {
    @UniqueConstraint(name = "ux_wf_action_exec_key", columnNames = {"tenant_id", "execution_key"})
  })
public class WorkflowActionExecution extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "instance_id", nullable = false)
  private WorkflowInstance instance;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "history_id")
  private WorkflowHistory history;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false, length = 60)
  private WorkflowActionType actionType;

  @Column(name = "execution_key", nullable = false, length = 180)
  private String executionKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private WorkflowExecutionStatus status;

  @Column(name = "attempt_count", nullable = false)
  private Integer attemptCount = 1;

  @Column(name = "request_json", columnDefinition = "TEXT")
  private String requestJson;

  @Column(name = "result_json", columnDefinition = "TEXT")
  private String resultJson;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "executed_at", nullable = false)
  private Instant executedAt;

  @Column(name = "executed_by", length = 120)
  private String executedBy;

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

  public WorkflowHistory getHistory() {
    return history;
  }

  public void setHistory(WorkflowHistory history) {
    this.history = history;
  }

  public WorkflowActionType getActionType() {
    return actionType;
  }

  public void setActionType(WorkflowActionType actionType) {
    this.actionType = actionType;
  }

  public String getExecutionKey() {
    return executionKey;
  }

  public void setExecutionKey(String executionKey) {
    this.executionKey = executionKey;
  }

  public WorkflowExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(WorkflowExecutionStatus status) {
    this.status = status;
  }

  public Integer getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(Integer attemptCount) {
    this.attemptCount = attemptCount;
  }

  public String getRequestJson() {
    return requestJson;
  }

  public void setRequestJson(String requestJson) {
    this.requestJson = requestJson;
  }

  public String getResultJson() {
    return resultJson;
  }

  public void setResultJson(String resultJson) {
    this.resultJson = resultJson;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Instant getExecutedAt() {
    return executedAt;
  }

  public void setExecutedAt(Instant executedAt) {
    this.executedAt = executedAt;
  }

  public String getExecutedBy() {
    return executedBy;
  }

  public void setExecutedBy(String executedBy) {
    this.executedBy = executedBy;
  }
}
