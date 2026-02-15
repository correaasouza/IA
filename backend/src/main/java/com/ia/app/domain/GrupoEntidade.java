package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "grupo_entidade")
public class GrupoEntidade extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "tipo_entidade_config_agrupador_id", nullable = false)
  private Long tipoEntidadeConfigAgrupadorId;

  @Column(name = "parent_id")
  private Long parentId;

  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  @Column(name = "nome_normalizado", nullable = false, length = 120)
  private String nomeNormalizado;

  @Column(name = "nivel", nullable = false)
  private Integer nivel = 0;

  @Column(name = "path", nullable = false, length = 900)
  private String path = "";

  @Column(name = "ordem", nullable = false)
  private Integer ordem = 0;

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

  public Long getTipoEntidadeConfigAgrupadorId() {
    return tipoEntidadeConfigAgrupadorId;
  }

  public void setTipoEntidadeConfigAgrupadorId(Long tipoEntidadeConfigAgrupadorId) {
    this.tipoEntidadeConfigAgrupadorId = tipoEntidadeConfigAgrupadorId;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getNomeNormalizado() {
    return nomeNormalizado;
  }

  public void setNomeNormalizado(String nomeNormalizado) {
    this.nomeNormalizado = nomeNormalizado;
  }

  public Integer getNivel() {
    return nivel;
  }

  public void setNivel(Integer nivel) {
    this.nivel = nivel;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Integer getOrdem() {
    return ordem;
  }

  public void setOrdem(Integer ordem) {
    this.ordem = ordem;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }
}
