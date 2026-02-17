package com.ia.app.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(
  name = "movimento_config",
  uniqueConstraints = {
    @UniqueConstraint(name = "ux_movimento_config_id_tenant", columnNames = {"id", "tenant_id"})
  })
public class MovimentoConfig extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  private Long tenantId;

  @Enumerated(EnumType.STRING)
  @Column(name = "movimento_tipo", nullable = false, length = 80)
  private MovimentoTipo tipoMovimento;

  @Column(name = "nome", nullable = false, length = 120)
  private String nome;

  @Column(name = "descricao", length = 255)
  private String descricao;

  @Column(name = "prioridade", nullable = false)
  private Integer prioridade = 100;

  @Column(name = "contexto_key", length = 120)
  private String contextoKey;

  @Column(name = "tipo_entidade_padrao_id", nullable = false)
  private Long tipoEntidadePadraoId;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  @Version
  @Column(name = "version", nullable = false)
  private Long version = 0L;

  @OrderBy("id ASC")
  @OneToMany(mappedBy = "movimentoConfig", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<MovimentoConfigEmpresa> empresas = new ArrayList<>();

  @OrderBy("id ASC")
  @OneToMany(mappedBy = "movimentoConfig", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<MovimentoConfigTipoEntidade> tiposEntidadePermitidos = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public MovimentoTipo getTipoMovimento() {
    return tipoMovimento;
  }

  public void setTipoMovimento(MovimentoTipo tipoMovimento) {
    this.tipoMovimento = tipoMovimento;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String descricao) {
    this.descricao = descricao;
  }

  public Integer getPrioridade() {
    return prioridade;
  }

  public void setPrioridade(Integer prioridade) {
    this.prioridade = prioridade;
  }

  public String getContextoKey() {
    return contextoKey;
  }

  public void setContextoKey(String contextoKey) {
    this.contextoKey = contextoKey;
  }

  public Long getTipoEntidadePadraoId() {
    return tipoEntidadePadraoId;
  }

  public void setTipoEntidadePadraoId(Long tipoEntidadePadraoId) {
    this.tipoEntidadePadraoId = tipoEntidadePadraoId;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public List<MovimentoConfigEmpresa> getEmpresas() {
    return empresas;
  }

  public List<MovimentoConfigTipoEntidade> getTiposEntidadePermitidos() {
    return tiposEntidadePermitidos;
  }

  public void replaceEmpresas(Collection<Long> empresaIds) {
    empresas.clear();
    if (empresaIds == null) {
      return;
    }
    Set<Long> unique = new LinkedHashSet<>(empresaIds);
    for (Long empresaId : unique) {
      if (empresaId == null) {
        continue;
      }
      MovimentoConfigEmpresa item = new MovimentoConfigEmpresa();
      item.setTenantId(tenantId);
      item.setEmpresaId(empresaId);
      item.setMovimentoConfig(this);
      empresas.add(item);
    }
  }

  public void replaceTiposEntidadePermitidos(Collection<Long> tipoEntidadeIds) {
    tiposEntidadePermitidos.clear();
    if (tipoEntidadeIds == null) {
      return;
    }
    Set<Long> unique = new LinkedHashSet<>(tipoEntidadeIds);
    for (Long tipoEntidadeId : unique) {
      if (tipoEntidadeId == null) {
        continue;
      }
      MovimentoConfigTipoEntidade item = new MovimentoConfigTipoEntidade();
      item.setTenantId(tenantId);
      item.setTipoEntidadeId(tipoEntidadeId);
      item.setMovimentoConfig(this);
      tiposEntidadePermitidos.add(item);
    }
  }
}
