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

  @Column(name = "tipo_entidade_padrao_id")
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

  @OrderBy("id ASC")
  @OneToMany(mappedBy = "movimentoConfig", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<MovimentoConfigItemTipo> tiposItensPermitidos = new ArrayList<>();

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

  public List<MovimentoConfigItemTipo> getTiposItensPermitidos() {
    return tiposItensPermitidos;
  }

  public void replaceEmpresas(Collection<Long> empresaIds) {
    Set<Long> desired = normalizeIds(empresaIds);
    empresas.removeIf(item -> item == null || !desired.contains(item.getEmpresaId()));

    Set<Long> current = new LinkedHashSet<>();
    for (MovimentoConfigEmpresa item : empresas) {
      if (item != null && item.getEmpresaId() != null) {
        current.add(item.getEmpresaId());
      }
    }

    for (Long empresaId : desired) {
      if (current.contains(empresaId)) {
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
    Set<Long> desired = normalizeIds(tipoEntidadeIds);
    tiposEntidadePermitidos.removeIf(item -> item == null || !desired.contains(item.getTipoEntidadeId()));

    Set<Long> current = new LinkedHashSet<>();
    for (MovimentoConfigTipoEntidade item : tiposEntidadePermitidos) {
      if (item != null && item.getTipoEntidadeId() != null) {
        current.add(item.getTipoEntidadeId());
      }
    }

    for (Long tipoEntidadeId : desired) {
      if (current.contains(tipoEntidadeId)) {
        continue;
      }
      MovimentoConfigTipoEntidade item = new MovimentoConfigTipoEntidade();
      item.setTenantId(tenantId);
      item.setTipoEntidadeId(tipoEntidadeId);
      item.setMovimentoConfig(this);
      tiposEntidadePermitidos.add(item);
    }
  }

  public void replaceTiposItensPermitidos(Collection<MovimentoConfigItemTipoInput> inputs) {
    List<MovimentoConfigItemTipoInput> normalized = normalizeItemTipoInputs(inputs);
    Set<Long> desiredIds = normalized.stream()
      .map(MovimentoConfigItemTipoInput::movimentoItemTipoId)
      .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    tiposItensPermitidos.removeIf(item -> item == null || !desiredIds.contains(item.getMovimentoItemTipoId()));

    java.util.Map<Long, MovimentoConfigItemTipo> currentByTipo = new java.util.LinkedHashMap<>();
    for (MovimentoConfigItemTipo item : tiposItensPermitidos) {
      if (item != null && item.getMovimentoItemTipoId() != null) {
        currentByTipo.put(item.getMovimentoItemTipoId(), item);
      }
    }

    for (MovimentoConfigItemTipoInput input : normalized) {
      MovimentoConfigItemTipo existing = currentByTipo.get(input.movimentoItemTipoId());
      if (existing != null) {
        existing.setCobrar(input.cobrar());
        continue;
      }
      MovimentoConfigItemTipo item = new MovimentoConfigItemTipo();
      item.setTenantId(tenantId);
      item.setMovimentoConfig(this);
      item.setMovimentoItemTipoId(input.movimentoItemTipoId());
      item.setCobrar(input.cobrar());
      tiposItensPermitidos.add(item);
    }
  }

  private Set<Long> normalizeIds(Collection<Long> values) {
    Set<Long> normalized = new LinkedHashSet<>();
    if (values == null) {
      return normalized;
    }
    for (Long value : values) {
      if (value != null) {
        normalized.add(value);
      }
    }
    return normalized;
  }

  private List<MovimentoConfigItemTipoInput> normalizeItemTipoInputs(Collection<MovimentoConfigItemTipoInput> values) {
    List<MovimentoConfigItemTipoInput> normalized = new ArrayList<>();
    if (values == null) {
      return normalized;
    }
    Set<Long> seen = new LinkedHashSet<>();
    for (MovimentoConfigItemTipoInput value : values) {
      if (value == null || value.movimentoItemTipoId() == null) {
        continue;
      }
      if (seen.add(value.movimentoItemTipoId())) {
        normalized.add(new MovimentoConfigItemTipoInput(value.movimentoItemTipoId(), value.cobrar()));
      }
    }
    return normalized;
  }

  public record MovimentoConfigItemTipoInput(Long movimentoItemTipoId, boolean cobrar) {}
}
