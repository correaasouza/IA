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
@Table(name = "entidade_info_rh")
public class EntidadeInfoRh extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false) private Long tenantId;
  @Column(name = "empresa_id", nullable = false) private Long empresaId;
  @Column(name = "registro_entidade_id", nullable = false) private Long registroEntidadeId;
  @Column(name = "atividades", length = 1000) private String atividades;
  @Column(name = "habilidades", length = 1000) private String habilidades;
  @Column(name = "experiencias", length = 1000) private String experiencias;
  @Column(name = "aceita_viajar", nullable = false) private boolean aceitaViajar;
  @Column(name = "possui_carro", nullable = false) private boolean possuiCarro;
  @Column(name = "possui_moto", nullable = false) private boolean possuiMoto;
  @Column(name = "meta_media_horas_vendidas_dia") private Long metaMediaHorasVendidasDia;
  @Column(name = "meta_produtos_vendidos", precision = 19, scale = 2) private BigDecimal metaProdutosVendidos;

  @Version
  @Column(name = "version", nullable = false) private Long version = 0L;

  public Long getId() { return id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getEmpresaId() { return empresaId; }
  public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
  public Long getRegistroEntidadeId() { return registroEntidadeId; }
  public void setRegistroEntidadeId(Long registroEntidadeId) { this.registroEntidadeId = registroEntidadeId; }
  public String getAtividades() { return atividades; }
  public void setAtividades(String atividades) { this.atividades = atividades; }
  public String getHabilidades() { return habilidades; }
  public void setHabilidades(String habilidades) { this.habilidades = habilidades; }
  public String getExperiencias() { return experiencias; }
  public void setExperiencias(String experiencias) { this.experiencias = experiencias; }
  public boolean isAceitaViajar() { return aceitaViajar; }
  public void setAceitaViajar(boolean aceitaViajar) { this.aceitaViajar = aceitaViajar; }
  public boolean isPossuiCarro() { return possuiCarro; }
  public void setPossuiCarro(boolean possuiCarro) { this.possuiCarro = possuiCarro; }
  public boolean isPossuiMoto() { return possuiMoto; }
  public void setPossuiMoto(boolean possuiMoto) { this.possuiMoto = possuiMoto; }
  public Long getMetaMediaHorasVendidasDia() { return metaMediaHorasVendidasDia; }
  public void setMetaMediaHorasVendidasDia(Long metaMediaHorasVendidasDia) { this.metaMediaHorasVendidasDia = metaMediaHorasVendidasDia; }
  public BigDecimal getMetaProdutosVendidos() { return metaProdutosVendidos; }
  public void setMetaProdutosVendidos(BigDecimal metaProdutosVendidos) { this.metaProdutosVendidos = metaProdutosVendidos; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
