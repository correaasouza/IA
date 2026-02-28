package com.ia.app.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "entidade_dados_fiscais")
public class EntidadeDadosFiscais extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false) private Long tenantId;
  @Column(name = "empresa_id", nullable = false) private Long empresaId;
  @Column(name = "registro_entidade_id", nullable = false) private Long registroEntidadeId;
  @Column(name = "manifestar_nota_automaticamente") private Short manifestarNotaAutomaticamente;
  @Column(name = "usa_nota_fiscal_fatura") private Short usaNotaFiscalFatura;
  @Column(name = "ignorar_importacao_nota") private Short ignorarImportacaoNota;

  @Version
  @Column(name = "version", nullable = false) private Long version = 0L;

  public Long getId() { return id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getEmpresaId() { return empresaId; }
  public void setEmpresaId(Long empresaId) { this.empresaId = empresaId; }
  public Long getRegistroEntidadeId() { return registroEntidadeId; }
  public void setRegistroEntidadeId(Long registroEntidadeId) { this.registroEntidadeId = registroEntidadeId; }
  public Short getManifestarNotaAutomaticamente() { return manifestarNotaAutomaticamente; }
  public void setManifestarNotaAutomaticamente(Short manifestarNotaAutomaticamente) { this.manifestarNotaAutomaticamente = manifestarNotaAutomaticamente; }
  public Short getUsaNotaFiscalFatura() { return usaNotaFiscalFatura; }
  public void setUsaNotaFiscalFatura(Short usaNotaFiscalFatura) { this.usaNotaFiscalFatura = usaNotaFiscalFatura; }
  public Short getIgnorarImportacaoNota() { return ignorarImportacaoNota; }
  public void setIgnorarImportacaoNota(Short ignorarImportacaoNota) { this.ignorarImportacaoNota = ignorarImportacaoNota; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
