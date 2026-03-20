package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "cep_cache")
public class CepCache extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cep", nullable = false, length = 8)
  private String cep;

  @Column(name = "logradouro", length = 200)
  private String logradouro;

  @Column(name = "bairro", length = 120)
  private String bairro;

  @Column(name = "localidade", length = 120)
  private String localidade;

  @Column(name = "uf", length = 2)
  private String uf;

  @Column(name = "ibge", length = 10)
  private String ibge;

  @Column(name = "origem", nullable = false, length = 20)
  private String origem = "EXTERNAL";

  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  public Long getId() { return id; }
  public String getCep() { return cep; }
  public void setCep(String cep) { this.cep = cep; }
  public String getLogradouro() { return logradouro; }
  public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
  public String getBairro() { return bairro; }
  public void setBairro(String bairro) { this.bairro = bairro; }
  public String getLocalidade() { return localidade; }
  public void setLocalidade(String localidade) { this.localidade = localidade; }
  public String getUf() { return uf; }
  public void setUf(String uf) { this.uf = uf; }
  public String getIbge() { return ibge; }
  public void setIbge(String ibge) { this.ibge = ibge; }
  public String getOrigem() { return origem; }
  public void setOrigem(String origem) { this.origem = origem; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
