package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tenant_unit")
public class TenantUnit extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "unidade_oficial_id", nullable = false)
  private UUID unidadeOficialId;

  @Column(name = "sigla", nullable = false, length = 20)
  private String sigla;

  @Column(name = "nome", nullable = false, length = 160)
  private String nome;

  @Column(name = "fator_para_oficial", nullable = false, precision = 24, scale = 12)
  private BigDecimal fatorParaOficial = BigDecimal.ONE;

  @Column(name = "system_mirror", nullable = false)
  private boolean systemMirror = false;

  @Column(name = "padrao", nullable = false)
  private boolean padrao = false;

  public UUID getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public UUID getUnidadeOficialId() {
    return unidadeOficialId;
  }

  public void setUnidadeOficialId(UUID unidadeOficialId) {
    this.unidadeOficialId = unidadeOficialId;
  }

  public String getSigla() {
    return sigla;
  }

  public void setSigla(String sigla) {
    this.sigla = sigla;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public BigDecimal getFatorParaOficial() {
    return fatorParaOficial;
  }

  public void setFatorParaOficial(BigDecimal fatorParaOficial) {
    this.fatorParaOficial = fatorParaOficial;
  }

  public boolean isSystemMirror() {
    return systemMirror;
  }

  public void setSystemMirror(boolean systemMirror) {
    this.systemMirror = systemMirror;
  }

  public boolean isPadrao() {
    return padrao;
  }

  public void setPadrao(boolean padrao) {
    this.padrao = padrao;
  }
}
