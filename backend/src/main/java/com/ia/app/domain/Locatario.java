package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "locatario")
public class Locatario extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  @Column(name = "data_limite_acesso", nullable = false)
  private LocalDate dataLimiteAcesso;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public LocalDate getDataLimiteAcesso() {
    return dataLimiteAcesso;
  }

  public void setDataLimiteAcesso(LocalDate dataLimiteAcesso) {
    this.dataLimiteAcesso = dataLimiteAcesso;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }
}
