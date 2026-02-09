package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "atalho_usuario")
public class AtalhoUsuario extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "user_id", nullable = false, length = 120)
  private String userId;

  @Column(name = "menu_id", nullable = false, length = 120)
  private String menuId;

  @Column(name = "icon", length = 60)
  private String icon;

  @Column(name = "ordem", nullable = false)
  private Integer ordem = 0;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getMenuId() {
    return menuId;
  }

  public void setMenuId(String menuId) {
    this.menuId = menuId;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public Integer getOrdem() {
    return ordem;
  }

  public void setOrdem(Integer ordem) {
    this.ordem = ordem;
  }
}
