package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contato_tipo")
public class ContatoTipo extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "codigo", nullable = false, length = 30)
  private String codigo;

  @Column(name = "nome", nullable = false, length = 80)
  private String nome;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  @Column(name = "obrigatorio", nullable = false)
  private boolean obrigatorio = false;

  @Column(name = "principal_unico", nullable = false)
  private boolean principalUnico = true;

  @Column(name = "mascara", length = 60)
  private String mascara;

  @Column(name = "regex_validacao", length = 200)
  private String regexValidacao;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigo) {
    this.codigo = codigo;
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

  public boolean isObrigatorio() {
    return obrigatorio;
  }

  public void setObrigatorio(boolean obrigatorio) {
    this.obrigatorio = obrigatorio;
  }

  public boolean isPrincipalUnico() {
    return principalUnico;
  }

  public void setPrincipalUnico(boolean principalUnico) {
    this.principalUnico = principalUnico;
  }

  public String getMascara() {
    return mascara;
  }

  public void setMascara(String mascara) {
    this.mascara = mascara;
  }

  public String getRegexValidacao() {
    return regexValidacao;
  }

  public void setRegexValidacao(String regexValidacao) {
    this.regexValidacao = regexValidacao;
  }
}
