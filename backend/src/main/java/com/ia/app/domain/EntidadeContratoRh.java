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
@Table(name = "entidade_contrato_rh")
public class EntidadeContratoRh extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false) private Long tenantId;
  @Column(name = "empresa_id", nullable = false) private Long empresaId;
  @Column(name = "registro_entidade_id", nullable = false) private Long registroEntidadeId;
  @Column(name = "numero", length = 40) private String numero;
  @Column(name = "admissao_data") private LocalDate admissaoData;
  @Column(name = "remuneracao", precision = 19, scale = 2) private BigDecimal remuneracao;
  @Column(name = "remuneracao_complementar", precision = 19, scale = 2) private BigDecimal remuneracaoComplementar;
  @Column(name = "bonificacao", precision = 19, scale = 2) private BigDecimal bonificacao;
  @Column(name = "sindicalizado", nullable = false) private boolean sindicalizado;
  @Column(name = "percentual_insalubridade", precision = 5, scale = 2) private BigDecimal percentualInsalubridade;
  @Column(name = "percentual_periculosidade", precision = 5, scale = 2) private BigDecimal percentualPericulosidade;
  @Column(name = "tipo_funcionario_id") private Long tipoFuncionarioId;
  @Column(name = "situacao_funcionario_id") private Long situacaoFuncionarioId;
  @Column(name = "setor_id") private Long setorId;
  @Column(name = "cargo_id") private Long cargoId;
  @Column(name = "ocupacao_atividade_id") private Long ocupacaoAtividadeId;

  @Version
  @Column(name = "version", nullable = false) private Long version = 0L;

  public Long getId() { return id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getEmpresaId() { return empresaId; }
  public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
  public Long getRegistroEntidadeId() { return registroEntidadeId; }
  public void setRegistroEntidadeId(Long registroEntidadeId) { this.registroEntidadeId = registroEntidadeId; }
  public String getNumero() { return numero; }
  public void setNumero(String numero) { this.numero = numero; }
  public LocalDate getAdmissaoData() { return admissaoData; }
  public void setAdmissaoData(LocalDate admissaoData) { this.admissaoData = admissaoData; }
  public BigDecimal getRemuneracao() { return remuneracao; }
  public void setRemuneracao(BigDecimal remuneracao) { this.remuneracao = remuneracao; }
  public BigDecimal getRemuneracaoComplementar() { return remuneracaoComplementar; }
  public void setRemuneracaoComplementar(BigDecimal remuneracaoComplementar) { this.remuneracaoComplementar = remuneracaoComplementar; }
  public BigDecimal getBonificacao() { return bonificacao; }
  public void setBonificacao(BigDecimal bonificacao) { this.bonificacao = bonificacao; }
  public boolean isSindicalizado() { return sindicalizado; }
  public void setSindicalizado(boolean sindicalizado) { this.sindicalizado = sindicalizado; }
  public BigDecimal getPercentualInsalubridade() { return percentualInsalubridade; }
  public void setPercentualInsalubridade(BigDecimal percentualInsalubridade) { this.percentualInsalubridade = percentualInsalubridade; }
  public BigDecimal getPercentualPericulosidade() { return percentualPericulosidade; }
  public void setPercentualPericulosidade(BigDecimal percentualPericulosidade) { this.percentualPericulosidade = percentualPericulosidade; }
  public Long getTipoFuncionarioId() { return tipoFuncionarioId; }
  public void setTipoFuncionarioId(Long tipoFuncionarioId) { this.tipoFuncionarioId = tipoFuncionarioId; }
  public Long getSituacaoFuncionarioId() { return situacaoFuncionarioId; }
  public void setSituacaoFuncionarioId(Long situacaoFuncionarioId) { this.situacaoFuncionarioId = situacaoFuncionarioId; }
  public Long getSetorId() { return setorId; }
  public void setSetorId(Long setorId) { this.setorId = setorId; }
  public Long getCargoId() { return cargoId; }
  public void setCargoId(Long cargoId) { this.cargoId = cargoId; }
  public Long getOcupacaoAtividadeId() { return ocupacaoAtividadeId; }
  public void setOcupacaoAtividadeId(Long ocupacaoAtividadeId) { this.ocupacaoAtividadeId = ocupacaoAtividadeId; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
