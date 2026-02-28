package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDate;

@Entity
@Table(
  name = "entidade_documentacao",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_ent_documentacao_scope_registro",
      columnNames = {"tenant_id", "empresa_id", "registro_entidade_id"})
  })
public class EntidadeDocumentacao extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "empresa_id", nullable = false)
  private Long empresaId;

  @Column(name = "registro_entidade_id", nullable = false)
  private Long registroEntidadeId;

  @Column(name = "tipo_registro_federal", nullable = false, length = 20)
  private String tipoRegistroFederal;

  @Column(name = "registro_federal", nullable = false, length = 40)
  private String registroFederal;

  @Column(name = "registro_federal_normalizado", nullable = false, length = 40)
  private String registroFederalNormalizado;

  @Column(name = "registro_federal_hash", nullable = false, length = 64)
  private String registroFederalHash;

  @Column(name = "registro_federal_data_emissao")
  private LocalDate registroFederalDataEmissao;

  @Column(name = "rg", length = 30)
  private String rg;

  @Column(name = "rg_tipo", length = 30)
  private String rgTipo;

  @Column(name = "rg_data_emissao")
  private LocalDate rgDataEmissao;

  @Column(name = "rg_uf_emissao", length = 2)
  private String rgUfEmissao;

  @Column(name = "registro_estadual", length = 40)
  private String registroEstadual;

  @Column(name = "registro_estadual_data_emissao")
  private LocalDate registroEstadualDataEmissao;

  @Column(name = "registro_estadual_uf", length = 2)
  private String registroEstadualUf;

  @Column(name = "registro_estadual_contribuinte", nullable = false)
  private boolean registroEstadualContribuinte;

  @Column(name = "registro_estadual_consumidor_final", nullable = false)
  private boolean registroEstadualConsumidorFinal;

  @Column(name = "registro_municipal", length = 40)
  private String registroMunicipal;

  @Column(name = "registro_municipal_data_emissao")
  private LocalDate registroMunicipalDataEmissao;

  @Column(name = "cnh", length = 20)
  private String cnh;

  @Column(name = "cnh_categoria", length = 5)
  private String cnhCategoria;

  @Column(name = "cnh_observacao", length = 255)
  private String cnhObservacao;

  @Column(name = "cnh_data_emissao")
  private LocalDate cnhDataEmissao;

  @Column(name = "suframa", length = 30)
  private String suframa;

  @Column(name = "rntc", length = 30)
  private String rntc;

  @Column(name = "pis", length = 20)
  private String pis;

  @Column(name = "titulo_eleitor", length = 20)
  private String tituloEleitor;

  @Column(name = "titulo_eleitor_zona", length = 10)
  private String tituloEleitorZona;

  @Column(name = "titulo_eleitor_secao", length = 10)
  private String tituloEleitorSecao;

  @Column(name = "ctps", length = 20)
  private String ctps;

  @Column(name = "ctps_serie", length = 20)
  private String ctpsSerie;

  @Column(name = "ctps_data_emissao")
  private LocalDate ctpsDataEmissao;

  @Column(name = "ctps_uf_emissao", length = 2)
  private String ctpsUfEmissao;

  @Column(name = "militar_numero", length = 40)
  private String militarNumero;

  @Column(name = "militar_serie", length = 40)
  private String militarSerie;

  @Column(name = "militar_categoria", length = 20)
  private String militarCategoria;

  @Column(name = "numero_nif", length = 40)
  private String numeroNif;

  @Column(name = "motivo_nao_nif")
  private Short motivoNaoNif;

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
  public String getTipoRegistroFederal() { return tipoRegistroFederal; }
  public void setTipoRegistroFederal(String tipoRegistroFederal) { this.tipoRegistroFederal = tipoRegistroFederal; }
  public String getRegistroFederal() { return registroFederal; }
  public void setRegistroFederal(String registroFederal) { this.registroFederal = registroFederal; }
  public String getRegistroFederalNormalizado() { return registroFederalNormalizado; }
  public void setRegistroFederalNormalizado(String registroFederalNormalizado) { this.registroFederalNormalizado = registroFederalNormalizado; }
  public String getRegistroFederalHash() { return registroFederalHash; }
  public void setRegistroFederalHash(String registroFederalHash) { this.registroFederalHash = registroFederalHash; }
  public LocalDate getRegistroFederalDataEmissao() { return registroFederalDataEmissao; }
  public void setRegistroFederalDataEmissao(LocalDate registroFederalDataEmissao) { this.registroFederalDataEmissao = registroFederalDataEmissao; }
  public String getRg() { return rg; }
  public void setRg(String rg) { this.rg = rg; }
  public String getRgTipo() { return rgTipo; }
  public void setRgTipo(String rgTipo) { this.rgTipo = rgTipo; }
  public LocalDate getRgDataEmissao() { return rgDataEmissao; }
  public void setRgDataEmissao(LocalDate rgDataEmissao) { this.rgDataEmissao = rgDataEmissao; }
  public String getRgUfEmissao() { return rgUfEmissao; }
  public void setRgUfEmissao(String rgUfEmissao) { this.rgUfEmissao = rgUfEmissao; }
  public String getRegistroEstadual() { return registroEstadual; }
  public void setRegistroEstadual(String registroEstadual) { this.registroEstadual = registroEstadual; }
  public LocalDate getRegistroEstadualDataEmissao() { return registroEstadualDataEmissao; }
  public void setRegistroEstadualDataEmissao(LocalDate registroEstadualDataEmissao) { this.registroEstadualDataEmissao = registroEstadualDataEmissao; }
  public String getRegistroEstadualUf() { return registroEstadualUf; }
  public void setRegistroEstadualUf(String registroEstadualUf) { this.registroEstadualUf = registroEstadualUf; }
  public boolean isRegistroEstadualContribuinte() { return registroEstadualContribuinte; }
  public void setRegistroEstadualContribuinte(boolean registroEstadualContribuinte) { this.registroEstadualContribuinte = registroEstadualContribuinte; }
  public boolean isRegistroEstadualConsumidorFinal() { return registroEstadualConsumidorFinal; }
  public void setRegistroEstadualConsumidorFinal(boolean registroEstadualConsumidorFinal) { this.registroEstadualConsumidorFinal = registroEstadualConsumidorFinal; }
  public String getRegistroMunicipal() { return registroMunicipal; }
  public void setRegistroMunicipal(String registroMunicipal) { this.registroMunicipal = registroMunicipal; }
  public LocalDate getRegistroMunicipalDataEmissao() { return registroMunicipalDataEmissao; }
  public void setRegistroMunicipalDataEmissao(LocalDate registroMunicipalDataEmissao) { this.registroMunicipalDataEmissao = registroMunicipalDataEmissao; }
  public String getCnh() { return cnh; }
  public void setCnh(String cnh) { this.cnh = cnh; }
  public String getCnhCategoria() { return cnhCategoria; }
  public void setCnhCategoria(String cnhCategoria) { this.cnhCategoria = cnhCategoria; }
  public String getCnhObservacao() { return cnhObservacao; }
  public void setCnhObservacao(String cnhObservacao) { this.cnhObservacao = cnhObservacao; }
  public LocalDate getCnhDataEmissao() { return cnhDataEmissao; }
  public void setCnhDataEmissao(LocalDate cnhDataEmissao) { this.cnhDataEmissao = cnhDataEmissao; }
  public String getSuframa() { return suframa; }
  public void setSuframa(String suframa) { this.suframa = suframa; }
  public String getRntc() { return rntc; }
  public void setRntc(String rntc) { this.rntc = rntc; }
  public String getPis() { return pis; }
  public void setPis(String pis) { this.pis = pis; }
  public String getTituloEleitor() { return tituloEleitor; }
  public void setTituloEleitor(String tituloEleitor) { this.tituloEleitor = tituloEleitor; }
  public String getTituloEleitorZona() { return tituloEleitorZona; }
  public void setTituloEleitorZona(String tituloEleitorZona) { this.tituloEleitorZona = tituloEleitorZona; }
  public String getTituloEleitorSecao() { return tituloEleitorSecao; }
  public void setTituloEleitorSecao(String tituloEleitorSecao) { this.tituloEleitorSecao = tituloEleitorSecao; }
  public String getCtps() { return ctps; }
  public void setCtps(String ctps) { this.ctps = ctps; }
  public String getCtpsSerie() { return ctpsSerie; }
  public void setCtpsSerie(String ctpsSerie) { this.ctpsSerie = ctpsSerie; }
  public LocalDate getCtpsDataEmissao() { return ctpsDataEmissao; }
  public void setCtpsDataEmissao(LocalDate ctpsDataEmissao) { this.ctpsDataEmissao = ctpsDataEmissao; }
  public String getCtpsUfEmissao() { return ctpsUfEmissao; }
  public void setCtpsUfEmissao(String ctpsUfEmissao) { this.ctpsUfEmissao = ctpsUfEmissao; }
  public String getMilitarNumero() { return militarNumero; }
  public void setMilitarNumero(String militarNumero) { this.militarNumero = militarNumero; }
  public String getMilitarSerie() { return militarSerie; }
  public void setMilitarSerie(String militarSerie) { this.militarSerie = militarSerie; }
  public String getMilitarCategoria() { return militarCategoria; }
  public void setMilitarCategoria(String militarCategoria) { this.militarCategoria = militarCategoria; }
  public String getNumeroNif() { return numeroNif; }
  public void setNumeroNif(String numeroNif) { this.numeroNif = numeroNif; }
  public Short getMotivoNaoNif() { return motivoNaoNif; }
  public void setMotivoNaoNif(Short motivoNaoNif) { this.motivoNaoNif = motivoNaoNif; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
