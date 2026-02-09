package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "registro_campo_valor")
public class RegistroCampoValor extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "registro_entidade_id", nullable = false)
  private Long registroEntidadeId;

  @Column(name = "campo_definicao_id", nullable = false)
  private Long campoDefinicaoId;

  @Column(name = "valor_texto")
  private String valorTexto;

  @Column(name = "valor_numero", precision = 18, scale = 2)
  private BigDecimal valorNumero;

  @Column(name = "valor_data")
  private LocalDate valorData;

  @Column(name = "valor_booleano")
  private Boolean valorBooleano;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public Long getRegistroEntidadeId() {
    return registroEntidadeId;
  }

  public void setRegistroEntidadeId(Long registroEntidadeId) {
    this.registroEntidadeId = registroEntidadeId;
  }

  public Long getCampoDefinicaoId() {
    return campoDefinicaoId;
  }

  public void setCampoDefinicaoId(Long campoDefinicaoId) {
    this.campoDefinicaoId = campoDefinicaoId;
  }

  public String getValorTexto() {
    return valorTexto;
  }

  public void setValorTexto(String valorTexto) {
    this.valorTexto = valorTexto;
  }

  public BigDecimal getValorNumero() {
    return valorNumero;
  }

  public void setValorNumero(BigDecimal valorNumero) {
    this.valorNumero = valorNumero;
  }

  public LocalDate getValorData() {
    return valorData;
  }

  public void setValorData(LocalDate valorData) {
    this.valorData = valorData;
  }

  public Boolean getValorBooleano() {
    return valorBooleano;
  }

  public void setValorBooleano(Boolean valorBooleano) {
    this.valorBooleano = valorBooleano;
  }
}
