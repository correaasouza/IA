package com.ia.app.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
  name = "agrupador_empresa",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_agrupador_empresa_config_nome",
      columnNames = {"tenant_id", "config_type", "config_id", "nome"})
  })
public class AgrupadorEmpresa extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "config_type", nullable = false, length = 80)
  private String configType;

  @Column(name = "config_id", nullable = false)
  private Long configId;

  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  @OneToMany(mappedBy = "agrupador", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private List<AgrupadorEmpresaItem> itens = new ArrayList<>();

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

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }

  public List<AgrupadorEmpresaItem> getItens() {
    return itens;
  }
}
