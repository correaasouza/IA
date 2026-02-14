package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "access_control_policy")
public class AccessControlPolicy extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "control_key", nullable = false, length = 160)
  private String controlKey;

  @Column(name = "roles_csv", nullable = false, length = 1000)
  private String rolesCsv = "";

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getControlKey() {
    return controlKey;
  }

  public void setControlKey(String controlKey) {
    this.controlKey = controlKey;
  }

  public String getRolesCsv() {
    return rolesCsv;
  }

  public void setRolesCsv(String rolesCsv) {
    this.rolesCsv = rolesCsv;
  }
}

