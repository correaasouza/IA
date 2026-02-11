package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "entidade_registro")
public class EntidadeRegistro extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "entidade_definicao_id", nullable = false)
  private Long entidadeDefinicaoId;

  @Column(name = "nome", nullable = false, length = 200)
  private String nome;

  @Column(name = "apelido", length = 200)
  private String apelido;

  @Column(name = "cpf_cnpj", nullable = false, length = 20)
  private String cpfCnpj;

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

  public Long getEntidadeDefinicaoId() {
    return entidadeDefinicaoId;
  }

  public void setEntidadeDefinicaoId(Long entidadeDefinicaoId) {
    this.entidadeDefinicaoId = entidadeDefinicaoId;
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

  public String getCpfCnpj() {
    return cpfCnpj;
  }

  public void setCpfCnpj(String cpfCnpj) {
    this.cpfCnpj = cpfCnpj;
  }

  public String getTipoPessoa() {
    return tipoPessoa;
  }

  public void setTipoPessoa(String tipoPessoa) {
    this.tipoPessoa = tipoPessoa;
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
