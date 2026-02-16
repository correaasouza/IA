package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogGroup;
import com.ia.app.dto.CatalogGroupRequest;
import com.ia.app.dto.CatalogGroupResponse;
import com.ia.app.dto.CatalogGroupUpdateRequest;
import com.ia.app.repository.CatalogGroupRepository;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.CatalogServiceItemRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogGroupService {

  private final CatalogGroupRepository repository;
  private final CatalogProductRepository productRepository;
  private final CatalogServiceItemRepository serviceItemRepository;
  private final CatalogItemContextService contextService;
  private final AuditService auditService;

  public CatalogGroupService(
      CatalogGroupRepository repository,
      CatalogProductRepository productRepository,
      CatalogServiceItemRepository serviceItemRepository,
      CatalogItemContextService contextService,
      AuditService auditService) {
    this.repository = repository;
    this.productRepository = productRepository;
    this.serviceItemRepository = serviceItemRepository;
    this.contextService = contextService;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<CatalogGroupResponse> tree(CatalogConfigurationType type) {
    var scope = contextService.resolveObrigatorio(type);
    List<CatalogGroup> groups = repository.findAllByTenantIdAndCatalogConfigurationIdAndAtivoTrueOrderByPathAsc(
      scope.tenantId(), scope.catalogConfigurationId());
    Map<Long, Long> directCountByGroupId = loadDirectCountByGroupId(
      type, scope.tenantId(), scope.catalogConfigurationId(), scope.agrupadorId());
    return toTree(groups, directCountByGroupId);
  }

  @Transactional
  public CatalogGroupResponse create(CatalogConfigurationType type, CatalogGroupRequest request) {
    var scope = contextService.resolveObrigatorio(type);
    String nome = normalizeNome(request.nome());
    String nomeNorm = normalizeNomeKey(nome);

    CatalogGroup parent = resolveParent(scope.tenantId(), scope.catalogConfigurationId(), request.parentId());
    Long parentId = parent == null ? null : parent.getId();
    int nivel = parent == null ? 0 : parent.getNivel() + 1;
    int ordem = (int) repository.countByTenantIdAndCatalogConfigurationIdAndParentIdAndAtivoTrue(
      scope.tenantId(), scope.catalogConfigurationId(), parentId) + 1;

    validateUniqueName(scope.tenantId(), scope.catalogConfigurationId(), parentId, nomeNorm, null);

    CatalogGroup entity = new CatalogGroup();
    entity.setTenantId(scope.tenantId());
    entity.setCatalogConfigurationId(scope.catalogConfigurationId());
    entity.setParentId(parentId);
    entity.setNome(nome);
    entity.setNomeNormalizado(nomeNorm);
    entity.setNivel(nivel);
    entity.setOrdem(ordem);
    entity.setPath("TMP");
    entity.setAtivo(true);
    CatalogGroup saved = repository.save(entity);

    String path = parent == null
      ? formatNode(saved.getId())
      : parent.getPath() + "/" + formatNode(saved.getId());
    saved.setPath(path);
    saved = repository.save(saved);

    auditService.log(scope.tenantId(), "CATALOG_GROUP_CREATED", "catalog_group", String.valueOf(saved.getId()),
      "type=" + type + ";catalogConfigurationId=" + scope.catalogConfigurationId() + ";nome=" + saved.getNome());
    return toResponse(saved);
  }

  @Transactional
  public CatalogGroupResponse update(CatalogConfigurationType type, Long groupId, CatalogGroupUpdateRequest request) {
    var scope = contextService.resolveObrigatorio(type);
    CatalogGroup entity = findAtivo(scope.tenantId(), scope.catalogConfigurationId(), groupId);
    String nome = normalizeNome(request.nome());
    String nomeNorm = normalizeNomeKey(nome);

    CatalogGroup newParent = resolveParent(scope.tenantId(), scope.catalogConfigurationId(), request.parentId());
    Long newParentId = newParent == null ? null : newParent.getId();
    if (newParentId != null && newParentId.equals(entity.getId())) {
      throw new IllegalArgumentException("catalog_group_ciclo_invalido");
    }
    if (newParent != null && newParent.getPath().startsWith(entity.getPath() + "/")) {
      throw new IllegalArgumentException("catalog_group_ciclo_invalido");
    }

    validateUniqueName(scope.tenantId(), scope.catalogConfigurationId(), newParentId, nomeNorm, entity.getId());

    String oldPath = entity.getPath();
    int oldNivel = entity.getNivel();
    int newNivel = newParent == null ? 0 : newParent.getNivel() + 1;
    String newPath = newParent == null
      ? formatNode(entity.getId())
      : newParent.getPath() + "/" + formatNode(entity.getId());

    entity.setNome(nome);
    entity.setNomeNormalizado(nomeNorm);
    entity.setParentId(newParentId);
    entity.setNivel(newNivel);
    entity.setPath(newPath);
    if (request.ordem() != null && request.ordem() > 0) {
      entity.setOrdem(request.ordem());
    }
    repository.save(entity);

    if (!oldPath.equals(newPath) || oldNivel != newNivel) {
      List<CatalogGroup> subtree = repository
        .findAllByTenantIdAndCatalogConfigurationIdAndPathStartingWithAndAtivoTrueOrderByPathAsc(
          scope.tenantId(), scope.catalogConfigurationId(), oldPath + "/");
      for (CatalogGroup child : subtree) {
        String suffix = child.getPath().substring((oldPath + "/").length());
        child.setPath(newPath + "/" + suffix);
        child.setNivel(child.getNivel() - oldNivel + newNivel);
      }
      repository.saveAll(subtree);
    }

    auditService.log(scope.tenantId(), "CATALOG_GROUP_UPDATED", "catalog_group", String.valueOf(entity.getId()),
      "type=" + type
        + ";catalogConfigurationId=" + scope.catalogConfigurationId()
        + ";nome=" + entity.getNome()
        + ";parentId=" + String.valueOf(entity.getParentId()));
    return toResponse(entity);
  }

  @Transactional
  public void delete(CatalogConfigurationType type, Long groupId) {
    var scope = contextService.resolveObrigatorio(type);
    CatalogGroup entity = findAtivo(scope.tenantId(), scope.catalogConfigurationId(), groupId);
    String pathPrefix = entity.getPath();
    List<CatalogGroup> subtree = new ArrayList<>();
    subtree.add(entity);
    subtree.addAll(repository.findAllByTenantIdAndCatalogConfigurationIdAndPathStartingWithAndAtivoTrueOrderByPathAsc(
      scope.tenantId(), scope.catalogConfigurationId(), pathPrefix + "/"));

    Set<Long> ids = subtree.stream().map(CatalogGroup::getId).collect(Collectors.toSet());
    boolean hasItems = switch (type) {
      case PRODUCTS ->
        productRepository.existsByTenantIdAndCatalogConfigurationIdAndCatalogGroupIdInAndAtivoTrue(
          scope.tenantId(), scope.catalogConfigurationId(), ids);
      case SERVICES ->
        serviceItemRepository.existsByTenantIdAndCatalogConfigurationIdAndCatalogGroupIdInAndAtivoTrue(
          scope.tenantId(), scope.catalogConfigurationId(), ids);
    };
    if (hasItems) {
      throw new IllegalArgumentException("catalog_group_possui_itens");
    }

    subtree.forEach(group -> group.setAtivo(false));
    repository.saveAll(subtree);

    auditService.log(scope.tenantId(), "CATALOG_GROUP_DELETED", "catalog_group", String.valueOf(entity.getId()),
      "type=" + type
        + ";catalogConfigurationId=" + scope.catalogConfigurationId()
        + ";nome=" + entity.getNome());
  }

  private List<CatalogGroupResponse> toTree(List<CatalogGroup> groups, Map<Long, Long> directCountByGroupId) {
    Map<Long, CatalogGroupResponse> map = new LinkedHashMap<>();
    List<CatalogGroupResponse> roots = new ArrayList<>();
    for (CatalogGroup entity : groups) {
      map.put(entity.getId(), toResponse(entity, directCountByGroupId.getOrDefault(entity.getId(), 0L)));
    }
    for (CatalogGroup entity : groups) {
      CatalogGroupResponse node = map.get(entity.getId());
      if (entity.getParentId() == null) {
        roots.add(node);
        continue;
      }
      CatalogGroupResponse parent = map.get(entity.getParentId());
      if (parent == null) {
        roots.add(node);
      } else {
        parent.getChildren().add(node);
      }
    }
    for (CatalogGroupResponse root : roots) {
      sumTreeCount(root);
    }
    return roots;
  }

  private CatalogGroupResponse toResponse(CatalogGroup entity) {
    return toResponse(entity, 0L);
  }

  private CatalogGroupResponse toResponse(CatalogGroup entity, Long directCount) {
    return new CatalogGroupResponse(
      entity.getId(),
      entity.getNome(),
      entity.getParentId(),
      entity.getNivel(),
      entity.getOrdem(),
      entity.getPath(),
      entity.isAtivo(),
      directCount == null ? 0L : directCount);
  }

  private long sumTreeCount(CatalogGroupResponse node) {
    long total = node.getTotalItems() == null ? 0L : node.getTotalItems();
    for (CatalogGroupResponse child : node.getChildren()) {
      total += sumTreeCount(child);
    }
    node.setTotalItems(total);
    return total;
  }

  private Map<Long, Long> loadDirectCountByGroupId(
      CatalogConfigurationType type,
      Long tenantId,
      Long catalogConfigurationId,
      Long agrupadorEmpresaId) {
    Map<Long, Long> map = new HashMap<>();
    if (type == CatalogConfigurationType.PRODUCTS) {
      for (CatalogProductRepository.CatalogGroupCountRow row :
          productRepository.countAtivosByGrupo(tenantId, catalogConfigurationId, agrupadorEmpresaId)) {
        if (row == null || row.getCatalogGroupId() == null) continue;
        map.put(row.getCatalogGroupId(), row.getTotal() == null ? 0L : row.getTotal());
      }
      return map;
    }
    for (CatalogServiceItemRepository.CatalogGroupCountRow row :
        serviceItemRepository.countAtivosByGrupo(tenantId, catalogConfigurationId, agrupadorEmpresaId)) {
      if (row == null || row.getCatalogGroupId() == null) continue;
      map.put(row.getCatalogGroupId(), row.getTotal() == null ? 0L : row.getTotal());
    }
    return map;
  }

  private CatalogGroup resolveParent(Long tenantId, Long catalogConfigurationId, Long parentId) {
    if (parentId == null) return null;
    return findAtivo(tenantId, catalogConfigurationId, parentId);
  }

  private CatalogGroup findAtivo(Long tenantId, Long catalogConfigurationId, Long id) {
    return repository.findByIdAndTenantIdAndCatalogConfigurationIdAndAtivoTrue(id, tenantId, catalogConfigurationId)
      .orElseThrow(() -> new EntityNotFoundException("catalog_group_not_found"));
  }

  private void validateUniqueName(
      Long tenantId,
      Long catalogConfigurationId,
      Long parentId,
      String nomeNormalizado,
      Long currentId) {
    boolean duplicated;
    if (currentId == null) {
      duplicated = repository.existsByTenantIdAndCatalogConfigurationIdAndParentIdAndNomeNormalizadoAndAtivoTrue(
        tenantId, catalogConfigurationId, parentId, nomeNormalizado);
    } else {
      duplicated = repository.existsByTenantIdAndCatalogConfigurationIdAndParentIdAndNomeNormalizadoAndAtivoTrueAndIdNot(
        tenantId, catalogConfigurationId, parentId, nomeNormalizado, currentId);
    }
    if (duplicated) {
      throw new IllegalArgumentException("catalog_group_nome_duplicado_mesmo_pai");
    }
  }

  private String normalizeNome(String nome) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("catalog_group_nome_required");
    }
    String value = nome.trim();
    if (value.length() > 120) {
      throw new IllegalArgumentException("catalog_group_nome_too_long");
    }
    return value;
  }

  private String normalizeNomeKey(String nome) {
    return nome.trim().toLowerCase(Locale.ROOT);
  }

  private String formatNode(Long id) {
    return String.format("%08d", id);
  }
}
