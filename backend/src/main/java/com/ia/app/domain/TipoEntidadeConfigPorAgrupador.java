package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
  name = "tipo_entidade_config_agrupador",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_tipo_ent_cfg_agrupador_ativo",
      columnNames = {"tenant_id", "tipo_entidade_id", "agrupador_id", "ativo"})
  })
public class TipoEntidadeConfigPorAgrupador extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "tipo_entidade_id", nullable = false)
  private Long tipoEntidadeId;

  @Column(name = "agrupador_id", nullable = false)
  private Long agrupadorId;

  @Column(name = "obrigar_um_telefone", nullable = false)
  private boolean obrigarUmTelefone;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getTipoEntidadeId() {
    return tipoEntidadeId;
  }

  public void setTipoEntidadeId(Long tipoEntidadeId) {
    this.tipoEntidadeId = tipoEntidadeId;
  }

  public Long getAgrupadorId() {
    return agrupadorId;
  }

  public void setAgrupadorId(Long agrupadorId) {
    this.agrupadorId = agrupadorId;
  }

  public boolean isObrigarUmTelefone() {
    return obrigarUmTelefone;
  }

  public void setObrigarUmTelefone(boolean obrigarUmTelefone) {
    this.obrigarUmTelefone = obrigarUmTelefone;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }
}
