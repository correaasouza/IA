package com.ia.app.domain;

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
  name = "agrupador_empresa_item",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_agrupador_empresa_item_config_empresa",
      columnNames = {"tenant_id", "config_type", "config_id", "empresa_id"}),
    @UniqueConstraint(
      name = "ux_agrupador_empresa_item_agrupador_empresa",
      columnNames = {"agrupador_id", "empresa_id"})
  })
public class AgrupadorEmpresaItem extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "config_type", nullable = false, length = 80)
  private String configType;

  @Column(name = "config_id", nullable = false)
  private Long configId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "agrupador_id", nullable = false)
  private AgrupadorEmpresa agrupador;

  @Column(name = "empresa_id", nullable = false)
  private Long empresaId;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getConfigType() {
    return configType;
  }

  public void setConfigType(String configType) {
    this.configType = configType;
  }

  public Long getConfigId() {
    return configId;
  }

  public void setConfigId(Long configId) {
    this.configId = configId;
  }

  public AgrupadorEmpresa getAgrupador() {
    return agrupador;
  }

  public void setAgrupador(AgrupadorEmpresa agrupador) {
    this.agrupador = agrupador;
  }

  public Long getEmpresaId() {
    return empresaId;
  }

  public void setEmpresaId(Long empresaId) {
    this.empresaId = empresaId;
  }
}
