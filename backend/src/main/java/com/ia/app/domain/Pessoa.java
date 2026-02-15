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
  name = "pessoa",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_pessoa_tenant_tipo_registro_federal_norm",
      columnNames = {"tenant_id", "tipo_registro", "registro_federal_normalizado"})
  })
public class Pessoa extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "nome", nullable = false, length = 200)
  private String nome;

  @Column(name = "apelido", length = 200)
  private String apelido;

  @Column(name = "cpf", length = 11)
  private String cpf;

  @Column(name = "cnpj", length = 14)
  private String cnpj;

  @Column(name = "id_estrangeiro", length = 40)
  private String idEstrangeiro;

  @Column(name = "tipo_registro", nullable = false, length = 20)
  private String tipoRegistro = "CPF";

  @Column(name = "registro_federal", nullable = false, length = 40)
  private String registroFederal;

  @Column(name = "registro_federal_normalizado", nullable = false, length = 40)
  private String registroFederalNormalizado;

  @Column(name = "tipo_pessoa", nullable = false, length = 20)
  private String tipoPessoa = "FISICA";

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  @Column(name = "versao", nullable = false)
  private Integer versao = 1;

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getApelido() {
    return apelido;
  }

  public void setApelido(String apelido) {
    this.apelido = apelido;
  }

  public String getCpf() {
    return cpf;
  }

  public void setCpf(String cpf) {
    this.cpf = cpf;
  }

  public String getCnpj() {
    return cnpj;
  }

  public void setCnpj(String cnpj) {
    this.cnpj = cnpj;
  }

  public String getIdEstrangeiro() {
    return idEstrangeiro;
  }

  public void setIdEstrangeiro(String idEstrangeiro) {
    this.idEstrangeiro = idEstrangeiro;
  }

  public String getTipoPessoa() {
    return tipoPessoa;
  }

  public void setTipoPessoa(String tipoPessoa) {
    this.tipoPessoa = tipoPessoa;
  }

  public String getTipoRegistro() {
    return tipoRegistro;
  }

  public void setTipoRegistro(String tipoRegistro) {
    this.tipoRegistro = tipoRegistro;
  }

  public String getRegistroFederal() {
    return registroFederal;
  }

  public void setRegistroFederal(String registroFederal) {
    this.registroFederal = registroFederal;
  }

  public String getRegistroFederalNormalizado() {
    return registroFederalNormalizado;
  }

  public void setRegistroFederalNormalizado(String registroFederalNormalizado) {
    this.registroFederalNormalizado = registroFederalNormalizado;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }

  public Integer getVersao() {
    return versao;
  }

  public void setVersao(Integer versao) {
    this.versao = versao;
  }
}
