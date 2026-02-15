package com.ia.app.service;

import com.ia.app.domain.TipoEntidade;
import com.ia.app.dto.TipoEntidadeRequest;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.TipoEntidadeConfigPorAgrupadorRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.repository.TipoEntidadeSpecifications;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TipoEntidadeService {

  private final TipoEntidadeRepository repository;
  private final AgrupadorEmpresaRepository agrupadorEmpresaRepository;
  private final TipoEntidadeConfigPorAgrupadorRepository configPorAgrupadorRepository;
  private final AuditService auditService;

  public TipoEntidadeService(
      TipoEntidadeRepository repository,
      AgrupadorEmpresaRepository agrupadorEmpresaRepository,
      TipoEntidadeConfigPorAgrupadorRepository configPorAgrupadorRepository,
      AuditService auditService) {
    this.repository = repository;
    this.agrupadorEmpresaRepository = agrupadorEmpresaRepository;
    this.configPorAgrupadorRepository = configPorAgrupadorRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public Page<TipoEntidade> list(String nome, Boolean ativo, Pageable pageable) {
    Long tenantId = requireTenant();
    Specification<TipoEntidade> spec = Specification
      .where(TipoEntidadeSpecifications.tenantEquals(tenantId))
      .and(TipoEntidadeSpecifications.nomeLike(normalizeFilter(nome)))
      .and(TipoEntidadeSpecifications.ativoEquals(ativo));
    return repository.findAll(spec, pageable);
  }

  @Transactional(readOnly = true)
  public TipoEntidade get(Long id) {
    return findByIdForTenant(id, requireTenant());
  }

  @Transactional
  public TipoEntidade create(TipoEntidadeRequest request) {
    Long tenantId = requireTenant();
    String nome = normalizeNome(request.nome());
    boolean ativo = Boolean.TRUE.equals(request.ativo());
    if (ativo && repository.existsByTenantIdAndNomeIgnoreCaseAndAtivoTrue(tenantId, nome)) {
      throw new IllegalArgumentException("tipo_entidade_nome_duplicado");
    }
    TipoEntidade entity = new TipoEntidade();
    entity.setTenantId(tenantId);
    entity.setNome(nome);
    entity.setTipoPadrao(false);
    entity.setCodigoSeed(null);
    entity.setAtivo(ativo);
    TipoEntidade saved = repository.save(entity);
    auditService.log(tenantId, "TIPO_ENTIDADE_CRIADO", "tipo_entidade", String.valueOf(saved.getId()),
      "nome=" + saved.getNome() + ";ativo=" + saved.isAtivo());
    return saved;
  }

  @Transactional
  public TipoEntidade update(Long id, TipoEntidadeRequest request) {
    Long tenantId = requireTenant();
    TipoEntidade entity = findByIdForTenant(id, tenantId);
    String nome = normalizeNome(request.nome());
    boolean ativo = Boolean.TRUE.equals(request.ativo());

    if (entity.isTipoPadrao() && !ativo) {
      throw new IllegalArgumentException("tipo_entidade_padrao_inativacao_nao_permitida");
    }

    boolean changedToActive = !entity.isAtivo() && ativo;
    boolean changedNome = !entity.getNome().equalsIgnoreCase(nome);
    if ((changedNome || changedToActive)
      && repository.existsByTenantIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(tenantId, nome, id)) {
      throw new IllegalArgumentException("tipo_entidade_nome_duplicado");
    }

    entity.setNome(nome);
    entity.setAtivo(entity.isTipoPadrao() || ativo);
    TipoEntidade saved = repository.save(entity);
    auditService.log(tenantId, "TIPO_ENTIDADE_ATUALIZADO", "tipo_entidade", String.valueOf(saved.getId()),
      "nome=" + saved.getNome() + ";ativo=" + saved.isAtivo());
    return saved;
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = requireTenant();
    TipoEntidade entity = findByIdForTenant(id, tenantId);
    if (entity.isTipoPadrao()) {
      throw new IllegalArgumentException("tipo_entidade_padrao_nao_excluivel");
    }

    configPorAgrupadorRepository.findAllByTenantIdAndTipoEntidadeIdAndAtivoTrue(tenantId, id).forEach(config -> {
      config.setAtivo(false);
      configPorAgrupadorRepository.save(config);
    });
    List<com.ia.app.domain.AgrupadorEmpresa> agrupadores = agrupadorEmpresaRepository
      .findAllByTenantIdAndConfigTypeAndConfigIdAndAtivoTrueOrderByNomeAsc(
        tenantId, ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE, id);
    if (!agrupadores.isEmpty()) {
      agrupadorEmpresaRepository.deleteAll(agrupadores);
    }

    entity.setAtivo(false);
    repository.save(entity);
    auditService.log(tenantId, "TIPO_ENTIDADE_EXCLUIDO", "tipo_entidade", String.valueOf(entity.getId()),
      "nome=" + entity.getNome());
  }

  private TipoEntidade findByIdForTenant(Long id, Long tenantId) {
    return repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("tipo_entidade_not_found"));
  }

  private String normalizeNome(String nome) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("tipo_entidade_nome_required");
    }
    String value = nome.trim();
    if (value.length() > 120) {
      throw new IllegalArgumentException("tipo_entidade_nome_too_long");
    }
    return value;
  }

  private String normalizeFilter(String nome) {
    if (nome == null) {
      return null;
    }
    String normalized = nome.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
