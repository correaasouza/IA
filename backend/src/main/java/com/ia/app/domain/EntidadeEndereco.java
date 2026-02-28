package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
@Table(name = "entidade_endereco")
public class EntidadeEndereco extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "empresa_id", nullable = false)
  private Long empresaId;

  @Column(name = "registro_entidade_id", nullable = false)
  private Long registroEntidadeId;

  @Column(name = "nome", length = 120)
  private String nome;

  @Column(name = "cep", length = 8)
  private String cep;

  @Column(name = "cep_estrangeiro", length = 20)
  private String cepEstrangeiro;

  @Column(name = "pais", length = 80)
  private String pais;

  @Column(name = "pais_codigo_ibge")
  private Long paisCodigoIbge;

  @Column(name = "uf", length = 2)
  private String uf;

  @Column(name = "uf_codigo_ibge", length = 10)
  private String ufCodigoIbge;

  @Column(name = "municipio", length = 120)
  private String municipio;

  @Column(name = "municipio_codigo_ibge", length = 10)
  private String municipioCodigoIbge;

  @Column(name = "logradouro", length = 200)
  private String logradouro;

  @Column(name = "logradouro_tipo", length = 40)
  private String logradouroTipo;

  @Column(name = "numero", length = 20)
  private String numero;

  @Column(name = "complemento", length = 120)
  private String complemento;

  @Column(name = "endereco_tipo", nullable = false, length = 20)
  private String enderecoTipo;

  @Column(name = "principal", nullable = false)
  private boolean principal;

  @Column(name = "longitude", precision = 10, scale = 7)
  private BigDecimal longitude;

  @Column(name = "latitude", precision = 10, scale = 7)
  private BigDecimal latitude;

  @Column(name = "estado_provincia_regiao_estrangeiro", length = 60)
  private String estadoProvinciaRegiaoEstrangeiro;

  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  public Long getId() { return id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getEmpresaId() { return empresaId; }
  public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
  public Long getRegistroEntidadeId() { return registroEntidadeId; }
  public void setRegistroEntidadeId(Long registroEntidadeId) { this.registroEntidadeId = registroEntidadeId; }
  public String getNome() { return nome; }
  public void setNome(String nome) { this.nome = nome; }
  public String getCep() { return cep; }
  public void setCep(String cep) { this.cep = cep; }
  public String getCepEstrangeiro() { return cepEstrangeiro; }
  public void setCepEstrangeiro(String cepEstrangeiro) { this.cepEstrangeiro = cepEstrangeiro; }
  public String getPais() { return pais; }
  public void setPais(String pais) { this.pais = pais; }
  public Long getPaisCodigoIbge() { return paisCodigoIbge; }
  public void setPaisCodigoIbge(Long paisCodigoIbge) { this.paisCodigoIbge = paisCodigoIbge; }
  public String getUf() { return uf; }
  public void setUf(String uf) { this.uf = uf; }
  public String getUfCodigoIbge() { return ufCodigoIbge; }
  public void setUfCodigoIbge(String ufCodigoIbge) { this.ufCodigoIbge = ufCodigoIbge; }
  public String getMunicipio() { return municipio; }
  public void setMunicipio(String municipio) { this.municipio = municipio; }
  public String getMunicipioCodigoIbge() { return municipioCodigoIbge; }
  public void setMunicipioCodigoIbge(String municipioCodigoIbge) { this.municipioCodigoIbge = municipioCodigoIbge; }
  public String getLogradouro() { return logradouro; }
  public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
  public String getLogradouroTipo() { return logradouroTipo; }
  public void setLogradouroTipo(String logradouroTipo) { this.logradouroTipo = logradouroTipo; }
  public String getNumero() { return numero; }
  public void setNumero(String numero) { this.numero = numero; }
  public String getComplemento() { return complemento; }
  public void setComplemento(String complemento) { this.complemento = complemento; }
  public String getEnderecoTipo() { return enderecoTipo; }
  public void setEnderecoTipo(String enderecoTipo) { this.enderecoTipo = enderecoTipo; }
  public boolean isPrincipal() { return principal; }
  public void setPrincipal(boolean principal) { this.principal = principal; }
  public BigDecimal getLongitude() { return longitude; }
  public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
  public BigDecimal getLatitude() { return latitude; }
  public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
  public String getEstadoProvinciaRegiaoEstrangeiro() { return estadoProvinciaRegiaoEstrangeiro; }
  public void setEstadoProvinciaRegiaoEstrangeiro(String estadoProvinciaRegiaoEstrangeiro) { this.estadoProvinciaRegiaoEstrangeiro = estadoProvinciaRegiaoEstrangeiro; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
