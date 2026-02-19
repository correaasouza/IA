package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.MovimentoItemTipo;
import com.ia.app.dto.MovimentoItemTipoRequest;
import com.ia.app.dto.MovimentoItemTipoResponse;
import com.ia.app.repository.MovimentoItemTipoRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimentoItemTipoService {

  private final MovimentoItemTipoRepository repository;
  private final AuditService auditService;

  public MovimentoItemTipoService(MovimentoItemTipoRepository repository, AuditService auditService) {
    this.repository = repository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public Page<MovimentoItemTipoResponse> list(String nome, CatalogConfigurationType catalogType, Boolean ativo, Pageable pageable) {
    Long tenantId = requireTenant();
    Specification<MovimentoItemTipo> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    String normalizedNome = normalizeOptionalText(nome);
    if (normalizedNome != null) {
      spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + normalizedNome.toLowerCase() + "%"));
    }
    if (catalogType != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("catalogType"), catalogType));
    }
    if (ativo != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("ativo"), ativo));
    }
    return repository.findAll(spec, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public MovimentoItemTipoResponse getById(Long id) {
    Long tenantId = requireTenant();
    return toResponse(findByIdForTenant(id, tenantId));
  }

  @Transactional
  public MovimentoItemTipoResponse create(MovimentoItemTipoRequest request) {
    Long tenantId = requireTenant();
    String nome = normalizeNome(request.nome());
    if (repository.existsByTenantIdAndNomeIgnoreCase(tenantId, nome)) {
      throw new IllegalArgumentException("movimento_item_tipo_nome_duplicado");
    }
    MovimentoItemTipo entity = new MovimentoItemTipo();
    entity.setTenantId(tenantId);
    entity.setNome(nome);
    entity.setCatalogType(requireCatalogType(request.catalogType()));
    entity.setAtivo(request.ativo() == null || request.ativo());

    MovimentoItemTipo saved = repository.saveAndFlush(entity);
    auditService.log(tenantId,
      "MOVIMENTO_ITEM_TIPO_CRIADO",
      "movimento_item_tipo",
      String.valueOf(saved.getId()),
      "nome=" + saved.getNome() + ";catalogType=" + saved.getCatalogType() + ";ativo=" + saved.isAtivo());
    return toResponse(saved);
  }

  @Transactional
  public MovimentoItemTipoResponse update(Long id, MovimentoItemTipoRequest request) {
    Long tenantId = requireTenant();
    MovimentoItemTipo entity = findByIdForTenant(id, tenantId);
    String nome = normalizeNome(request.nome());
    if (repository.existsByTenantIdAndNomeIgnoreCaseAndIdNot(tenantId, nome, id)) {
      throw new IllegalArgumentException("movimento_item_tipo_nome_duplicado");
    }
    entity.setNome(nome);
    entity.setCatalogType(requireCatalogType(request.catalogType()));
    entity.setAtivo(request.ativo() == null || request.ativo());

    MovimentoItemTipo saved = repository.saveAndFlush(entity);
    auditService.log(tenantId,
      "MOVIMENTO_ITEM_TIPO_ATUALIZADO",
      "movimento_item_tipo",
      String.valueOf(saved.getId()),
      "nome=" + saved.getNome() + ";catalogType=" + saved.getCatalogType() + ";ativo=" + saved.isAtivo());
    return toResponse(saved);
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = requireTenant();
    MovimentoItemTipo entity = findByIdForTenant(id, tenantId);
    if (!entity.isAtivo()) {
      return;
    }
    entity.setAtivo(false);
    repository.save(entity);
    auditService.log(tenantId,
      "MOVIMENTO_ITEM_TIPO_INATIVADO",
      "movimento_item_tipo",
      String.valueOf(entity.getId()),
      "nome=" + entity.getNome());
  }

  @Transactional(readOnly = true)
  public MovimentoItemTipo requireById(Long id) {
    Long tenantId = requireTenant();
    return findByIdForTenant(id, tenantId);
  }

  @Transactional(readOnly = true)
  public MovimentoItemTipo requireActiveById(Long id) {
    Long tenantId = requireTenant();
    MovimentoItemTipo entity = findByIdForTenant(id, tenantId);
    if (!entity.isAtivo()) {
      throw new IllegalArgumentException("movimento_item_tipo_inativo");
    }
    return entity;
  }

  private MovimentoItemTipo findByIdForTenant(Long id, Long tenantId) {
    if (id == null || id <= 0) {
      throw new IllegalArgumentException("movimento_item_tipo_id_invalid");
    }
    return repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("movimento_item_tipo_not_found"));
  }

  private CatalogConfigurationType requireCatalogType(CatalogConfigurationType catalogType) {
    if (catalogType == null) {
      throw new IllegalArgumentException("movimento_item_tipo_catalog_type_required");
    }
    return catalogType;
  }

  private String normalizeNome(String nome) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("movimento_item_tipo_nome_required");
    }
    String normalized = nome.trim();
    return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
  }

  private String normalizeOptionalText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private MovimentoItemTipoResponse toResponse(MovimentoItemTipo entity) {
    return new MovimentoItemTipoResponse(
      entity.getId(),
      entity.getNome(),
      entity.getCatalogType(),
      entity.isAtivo(),
      entity.getVersion(),
      entity.getCreatedAt(),
      entity.getUpdatedAt());
  }
}
