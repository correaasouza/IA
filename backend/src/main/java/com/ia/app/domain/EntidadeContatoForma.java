package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "entidade_contato_forma")
public class EntidadeContatoForma extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Column(name = "empresa_id", nullable = false)
  private Long empresaId;

  @Column(name = "registro_entidade_id", nullable = false)
  private Long registroEntidadeId;

  @Column(name = "contato_id", nullable = false)
  private Long contatoId;

  @Column(name = "tipo_contato", nullable = false, length = 30)
  private String tipoContato;

  @Column(name = "valor", nullable = false, length = 200)
  private String valor;

  @Column(name = "valor_normalizado", nullable = false, length = 200)
  private String valorNormalizado;

  @Column(name = "preferencial", nullable = false)
  private boolean preferencial;

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
  public Long getContatoId() { return contatoId; }
  public void setContatoId(Long contatoId) { this.contatoId = contatoId; }
  public String getTipoContato() { return tipoContato; }
  public void setTipoContato(String tipoContato) { this.tipoContato = tipoContato; }
  public String getValor() { return valor; }
  public void setValor(String valor) { this.valor = valor; }
  public String getValorNormalizado() { return valorNormalizado; }
  public void setValorNormalizado(String valorNormalizado) { this.valorNormalizado = valorNormalizado; }
  public boolean isPreferencial() { return preferencial; }
  public void setPreferencial(boolean preferencial) { this.preferencial = preferencial; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
