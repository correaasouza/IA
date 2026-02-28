package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "entidade_info_comercial")
public class EntidadeInfoComercial extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false) private Long tenantId;
  @Column(name = "empresa_id", nullable = false) private Long empresaId;
  @Column(name = "registro_entidade_id", nullable = false) private Long registroEntidadeId;
  @Column(name = "faturamento_dia_inicial") private LocalDate faturamentoDiaInicial;
  @Column(name = "faturamento_dia_final") private LocalDate faturamentoDiaFinal;
  @Column(name = "faturamento_dias_prazo") private Integer faturamentoDiasPrazo;
  @Column(name = "boletos_enviar_email", nullable = false) private boolean boletosEnviarEmail;
  @Column(name = "faturamento_frequencia_cobranca_id") private Long faturamentoFrequenciaCobrancaId;
  @Column(name = "juro_taxa_padrao", precision = 5, scale = 2) private BigDecimal juroTaxaPadrao;
  @Column(name = "ramo_atividade", length = 100) private String ramoAtividade;
  @Column(name = "consumidor_final", nullable = false) private boolean consumidorFinal;

  @Version
  @Column(name = "version", nullable = false) private Long version = 0L;

  public Long getId() { return id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getEmpresaId() { return empresaId; }
  public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
  public Long getRegistroEntidadeId() { return registroEntidadeId; }
  public void setRegistroEntidadeId(Long registroEntidadeId) { this.registroEntidadeId = registroEntidadeId; }
  public LocalDate getFaturamentoDiaInicial() { return faturamentoDiaInicial; }
  public void setFaturamentoDiaInicial(LocalDate faturamentoDiaInicial) { this.faturamentoDiaInicial = faturamentoDiaInicial; }
  public LocalDate getFaturamentoDiaFinal() { return faturamentoDiaFinal; }
  public void setFaturamentoDiaFinal(LocalDate faturamentoDiaFinal) { this.faturamentoDiaFinal = faturamentoDiaFinal; }
  public Integer getFaturamentoDiasPrazo() { return faturamentoDiasPrazo; }
  public void setFaturamentoDiasPrazo(Integer faturamentoDiasPrazo) { this.faturamentoDiasPrazo = faturamentoDiasPrazo; }
  public boolean isBoletosEnviarEmail() { return boletosEnviarEmail; }
  public void setBoletosEnviarEmail(boolean boletosEnviarEmail) { this.boletosEnviarEmail = boletosEnviarEmail; }
  public Long getFaturamentoFrequenciaCobrancaId() { return faturamentoFrequenciaCobrancaId; }
  public void setFaturamentoFrequenciaCobrancaId(Long faturamentoFrequenciaCobrancaId) { this.faturamentoFrequenciaCobrancaId = faturamentoFrequenciaCobrancaId; }
  public BigDecimal getJuroTaxaPadrao() { return juroTaxaPadrao; }
  public void setJuroTaxaPadrao(BigDecimal juroTaxaPadrao) { this.juroTaxaPadrao = juroTaxaPadrao; }
  public String getRamoAtividade() { return ramoAtividade; }
  public void setRamoAtividade(String ramoAtividade) { this.ramoAtividade = ramoAtividade; }
  public boolean isConsumidorFinal() { return consumidorFinal; }
  public void setConsumidorFinal(boolean consumidorFinal) { this.consumidorFinal = consumidorFinal; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
