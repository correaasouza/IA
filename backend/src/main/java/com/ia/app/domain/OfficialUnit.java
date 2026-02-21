package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "official_unit")
public class OfficialUnit extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "codigo_oficial", nullable = false, length = 20)
  private String codigoOficial;

  @Column(name = "descricao", nullable = false, length = 160)
  private String descricao;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  @Enumerated(EnumType.STRING)
  @Column(name = "origem", nullable = false, length = 60)
  private OfficialUnitOrigin origem = OfficialUnitOrigin.MANUAL;

  public UUID getId() {
    return id;
  }

  public String getCodigoOficial() {
    return codigoOficial;
  }

  public void setCodigoOficial(String codigoOficial) {
    this.codigoOficial = codigoOficial;
  }

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String descricao) {
    this.descricao = descricao;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }

  public OfficialUnitOrigin getOrigem() {
    return origem;
  }

  public void setOrigem(OfficialUnitOrigin origem) {
    this.origem = origem;
  }
}
