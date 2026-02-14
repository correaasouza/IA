package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario_locatario_acesso")
public class UsuarioLocatarioAcesso extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "usuario_id", nullable = false, length = 120)
  private String usuarioId;

  @Column(name = "locatario_id", nullable = false)
  private Long locatarioId;

  public Long getId() {
    return id;
  }

  public String getUsuarioId() {
    return usuarioId;
  }

  public void setUsuarioId(String usuarioId) {
    this.usuarioId = usuarioId;
  }

  public Long getLocatarioId() {
    return locatarioId;
  }

  public void setLocatarioId(Long locatarioId) {
    this.locatarioId = locatarioId;
  }
}

