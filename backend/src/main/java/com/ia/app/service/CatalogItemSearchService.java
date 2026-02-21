package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogGroup;
import com.ia.app.domain.MovimentoConfig;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.dto.CatalogGroupTreeNodeDTO;
import com.ia.app.dto.CatalogItemSummaryDTO;
import com.ia.app.dto.MovimentoConfigItemTipoResponse;
import com.ia.app.repository.CatalogGroupRepository;
import com.ia.app.repository.CatalogItemSearchRepository;
import com.ia.app.repository.MovimentoConfigRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogItemSearchService {

  private static final int DEFAULT_SIZE = 30;
  private static final int MAX_SIZE = 100;

  private final MovimentoConfigRepository movimentoConfigRepository;
  private final MovimentoConfigItemTipoService movimentoConfigItemTipoService;
  private final CatalogItemContextService catalogItemContextService;
  private final CatalogItemSearchRepository catalogItemSearchRepository;
  private final CatalogGroupRepository catalogGroupRepository;

  public CatalogItemSearchService(
      MovimentoConfigRepository movimentoConfigRepository,
      MovimentoConfigItemTipoService movimentoConfigItemTipoService,
      CatalogItemContextService catalogItemContextService,
      CatalogItemSearchRepository catalogItemSearchRepository,
      CatalogGroupRepository catalogGroupRepository) {
    this.movimentoConfigRepository = movimentoConfigRepository;
    this.movimentoConfigItemTipoService = movimentoConfigItemTipoService;
    this.catalogItemContextService = catalogItemContextService;
    this.catalogItemSearchRepository = catalogItemSearchRepository;
    this.catalogGroupRepository = catalogGroupRepository;
  }

  @Transactional(readOnly = true)
  public Page<CatalogItemSummaryDTO> search(
      String movementType,
      Long movementConfigId,
      Long movementItemTypeId,
      String q,
      Long groupId,
      boolean includeDescendants,
      Boolean ativo,
      Pageable pageable) {
    ResolvedScope scope = resolveScope(movementType, movementConfigId, movementItemTypeId);
    Pageable normalizedPageable = normalizePageable(pageable);
    String normalizedQuery = normalizeOptionalText(q);
    Boolean normalizedAtivo = ativo == null ? Boolean.TRUE : ativo;

    String groupPath = null;
    if (groupId != null && groupId > 0) {
      CatalogGroup group = findGroup(scope.catalogScope().tenantId(), scope.catalogScope().catalogConfigurationId(), groupId);
      groupPath = group.getPath();
    }

    CatalogItemSearchRepository.SearchCriteria criteria = new CatalogItemSearchRepository.SearchCriteria(
      scope.catalogScope().tenantId(),
      scope.catalogScope().catalogConfigurationId(),
      scope.catalogScope().agrupadorId(),
      normalizedQuery,
      normalizedQuery == null ? null : "%" + normalizedQuery.toLowerCase() + "%",
      groupId,
      includeDescendants,
      groupPath,
      groupPath == null ? null : groupPath + "/%",
      normalizedAtivo,
      normalizedPageable.getPageNumber(),
      normalizedPageable.getPageSize());

    Page<CatalogItemSearchRepository.SearchRow> page = catalogItemSearchRepository.search(scope.catalogType(), criteria);
    Map<Long, String> groupNameById = loadGroupNameByPath(page.getContent().stream().map(CatalogItemSearchRepository.SearchRow::groupPath).toList(),
      scope.catalogScope().tenantId(),
      scope.catalogScope().catalogConfigurationId());

    List<CatalogItemSummaryDTO> content = page.getContent().stream().map(row -> new CatalogItemSummaryDTO(
      row.id(),
      scope.catalogType().name(),
      row.codigo(),
      row.nome(),
      row.descricao(),
      row.groupId(),
      row.groupPath(),
      buildBreadcrumb(row.groupPath(), groupNameById),
      row.ativo(),
      null,
      null,
      null
    )).toList();

    return new PageImpl<>(content, normalizedPageable, page.getTotalElements());
  }

  @Transactional(readOnly = true)
  public List<CatalogGroupTreeNodeDTO> tree(
      String movementType,
      Long movementConfigId,
      Long movementItemTypeId,
      Long parentId) {
    ResolvedScope scope = resolveScope(movementType, movementConfigId, movementItemTypeId);
    Long tenantId = scope.catalogScope().tenantId();
    Long catalogConfigurationId = scope.catalogScope().catalogConfigurationId();

    if (parentId != null && parentId > 0) {
      findGroup(tenantId, catalogConfigurationId, parentId);
    }

    List<CatalogGroup> rows = parentId == null
      ? catalogGroupRepository.findAllByTenantIdAndCatalogConfigurationIdAndParentIdIsNullAndAtivoTrueOrderByOrdemAscNomeAsc(
        tenantId,
        catalogConfigurationId)
      : catalogGroupRepository.findAllByTenantIdAndCatalogConfigurationIdAndParentIdAndAtivoTrueOrderByOrdemAscNomeAsc(
        tenantId,
        catalogConfigurationId,
        parentId);

    Map<Long, String> groupNameById = loadGroupNameByPath(rows.stream().map(CatalogGroup::getPath).toList(), tenantId, catalogConfigurationId);
    List<CatalogGroupTreeNodeDTO> response = new ArrayList<>(rows.size());
    for (CatalogGroup item : rows) {
      boolean hasChildren = catalogGroupRepository.existsByTenantIdAndCatalogConfigurationIdAndParentIdAndAtivoTrue(
        tenantId,
        catalogConfigurationId,
        item.getId());
      response.add(new CatalogGroupTreeNodeDTO(
        item.getId(),
        item.getNome(),
        item.getParentId(),
        item.getNivel(),
        item.getPath(),
        hasChildren,
        buildBreadcrumb(item.getPath(), groupNameById)));
    }
    return response;
  }

  private ResolvedScope resolveScope(String movementType, Long movementConfigId, Long movementItemTypeId) {
    MovimentoTipo tipo = MovimentoTipo.from(movementType);
    if (tipo != MovimentoTipo.MOVIMENTO_ESTOQUE) {
      throw new IllegalArgumentException("movimento_tipo_nao_implementado");
    }
    Long configId = requirePositive(movementConfigId, "movimento_config_id_invalid");
    Long itemTypeId = requirePositive(movementItemTypeId, "movimento_item_tipo_id_invalid");

    MovimentoConfig config = movimentoConfigRepository.findByIdAndTenantId(configId, requireTenant())
      .orElseThrow(() -> new EntityNotFoundException("movimento_config_not_found"));
    if (config.getTipoMovimento() != tipo) {
      throw new IllegalArgumentException("movimento_tipo_invalid");
    }

    MovimentoConfigItemTipoResponse vinculo = movimentoConfigItemTipoService.listAtivosForConfig(configId).stream()
      .filter(item -> item.movimentoItemTipoId() != null && item.movimentoItemTipoId().equals(itemTypeId))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_item_tipo_nao_habilitado"));

    CatalogItemContextService.CatalogItemScope catalogScope = catalogItemContextService.resolveObrigatorio(vinculo.catalogType());
    return new ResolvedScope(tipo, config, vinculo.catalogType(), catalogScope);
  }

  private CatalogGroup findGroup(Long tenantId, Long catalogConfigurationId, Long groupId) {
    if (groupId == null || groupId <= 0) {
      throw new IllegalArgumentException("catalog_group_id_invalid");
    }
    return catalogGroupRepository.findByIdAndTenantIdAndCatalogConfigurationIdAndAtivoTrue(groupId, tenantId, catalogConfigurationId)
      .orElseThrow(() -> new EntityNotFoundException("catalog_group_not_found"));
  }

  private Pageable normalizePageable(Pageable pageable) {
    if (pageable == null) {
      return PageRequest.of(0, DEFAULT_SIZE);
    }
    int page = Math.max(0, pageable.getPageNumber());
    int size = pageable.getPageSize() <= 0 ? DEFAULT_SIZE : Math.min(pageable.getPageSize(), MAX_SIZE);
    return PageRequest.of(page, size);
  }

  private String normalizeOptionalText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private Long requirePositive(Long value, String errorCode) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException(errorCode);
    }
    return value;
  }

  private Long requireTenant() {
    Long tenantId = com.ia.app.tenant.TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private Map<Long, String> loadGroupNameByPath(List<String> paths, Long tenantId, Long catalogConfigurationId) {
    Set<Long> ids = new LinkedHashSet<>();
    for (String path : paths) {
      ids.addAll(parsePathIds(path));
    }
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<Long, String> map = new HashMap<>();
    for (CatalogGroup group : catalogGroupRepository.findAllByTenantIdAndCatalogConfigurationIdAndIdIn(tenantId, catalogConfigurationId, ids)) {
      map.put(group.getId(), group.getNome());
    }
    return map;
  }

  private Set<Long> parsePathIds(String path) {
    Set<Long> ids = new LinkedHashSet<>();
    if (path == null || path.isBlank()) {
      return ids;
    }
    String[] parts = path.split("/");
    for (String part : parts) {
      if (part == null || part.isBlank()) {
        continue;
      }
      try {
        ids.add(Long.parseLong(part));
      } catch (NumberFormatException ignored) {
      }
    }
    return ids;
  }

  private String buildBreadcrumb(String path, Map<Long, String> groupNameById) {
    if (path == null || path.isBlank()) {
      return null;
    }
    List<String> names = new ArrayList<>();
    for (Long id : parsePathIds(path)) {
      String name = groupNameById.get(id);
      if (name != null && !name.isBlank()) {
        names.add(name);
      }
    }
    return names.isEmpty() ? null : String.join(" / ", names);
  }

  private record ResolvedScope(
    MovimentoTipo movementType,
    MovimentoConfig movimentoConfig,
    CatalogConfigurationType catalogType,
    CatalogItemContextService.CatalogItemScope catalogScope
  ) {}
}
