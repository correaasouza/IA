package com.ia.app.workflow.domain;

import com.ia.app.domain.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
  name = "workflow_definition",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_workflow_def_tenant_origin_ctx_version",
      columnNames = {"tenant_id", "origin", "context_type", "context_id", "version_num"})
  })
public class WorkflowDefinition extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "origin", nullable = false, length = 60)
  private WorkflowOrigin origin;

  @Enumerated(EnumType.STRING)
  @Column(name = "context_type", length = 60)
  private WorkflowDefinitionContextType contextType;

  @Column(name = "context_id")
  private Long contextId;

  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Column(name = "version_num", nullable = false)
  private Integer versionNum;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private WorkflowDefinitionStatus status = WorkflowDefinitionStatus.PUBLISHED;

  @Column(name = "description", length = 255)
  private String description;

  @Column(name = "layout_json", columnDefinition = "TEXT")
  private String layoutJson;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "published_by", length = 120)
  private String publishedBy;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Version
  @Column(name = "entity_version", nullable = false)
  private Long entityVersion = 0L;

  @OrderBy("id ASC")
  @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<WorkflowState> states = new ArrayList<>();

  @OrderBy("priority ASC, id ASC")
  @OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<WorkflowTransition> transitions = new ArrayList<>();

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

  public WorkflowDefinitionContextType getContextType() {
    return contextType;
  }

  public void setContextType(WorkflowDefinitionContextType contextType) {
    this.contextType = contextType;
  }

  public Long getContextId() {
    return contextId;
  }

  public void setContextId(Long contextId) {
    this.contextId = contextId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getVersionNum() {
    return versionNum;
  }

  public void setVersionNum(Integer versionNum) {
    this.versionNum = versionNum;
  }

  public WorkflowDefinitionStatus getStatus() {
    return status;
  }

  public void setStatus(WorkflowDefinitionStatus status) {
    this.status = status;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLayoutJson() {
    return layoutJson;
  }

  public void setLayoutJson(String layoutJson) {
    this.layoutJson = layoutJson;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(Instant publishedAt) {
    this.publishedAt = publishedAt;
  }

  public String getPublishedBy() {
    return publishedBy;
  }

  public void setPublishedBy(String publishedBy) {
    this.publishedBy = publishedBy;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Long getEntityVersion() {
    return entityVersion;
  }

  public void setEntityVersion(Long entityVersion) {
    this.entityVersion = entityVersion;
  }

  public List<WorkflowState> getStates() {
    return states;
  }

  public List<WorkflowTransition> getTransitions() {
    return transitions;
  }
}
