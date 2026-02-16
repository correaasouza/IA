package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogGroup;
import com.ia.app.domain.CatalogItemBase;
import com.ia.app.domain.CatalogNumberingMode;
import com.ia.app.domain.CatalogProduct;
import com.ia.app.domain.CatalogServiceItem;
import com.ia.app.dto.CatalogItemRequest;
import com.ia.app.dto.CatalogItemResponse;
import com.ia.app.repository.CatalogGroupRepository;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.CatalogServiceItemRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogItemCrudSupportService {

  private final CatalogItemContextService contextService;
  private final CatalogItemCodeService codeService;
  private final CatalogGroupRepository groupRepository;
  private final CatalogProductRepository productRepository;
  private final CatalogServiceItemRepository serviceItemRepository;
  private final AuditService auditService;

  public CatalogItemCrudSupportService(
      CatalogItemContextService contextService,
      CatalogItemCodeService codeService,
      CatalogGroupRepository groupRepository,
      CatalogProductRepository productRepository,
      CatalogServiceItemRepository serviceItemRepository,
      AuditService auditService) {
    this.contextService = contextService;
    this.codeService = codeService;
    this.groupRepository = groupRepository;
    this.productRepository = productRepository;
    this.serviceItemRepository = serviceItemRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public Page<CatalogItemResponse> list(
      CatalogConfigurationType type,
      Long codigo,
      String text,
      Long catalogGroupId,
      Boolean ativo,
      Pageable pageable) {
    var scope = contextService.resolveObrigatorio(type);
    Long normalizedCodigo = normalizeCodigo(codigo);
    String normalizedText = normalizeText(text);

    Page<? extends CatalogItemBase> page = switch (type) {
      case PRODUCTS -> productRepository.search(
        scope.tenantId(),
        scope.catalogConfigurationId(),
        scope.agrupadorId(),
        normalizedCodigo,
        normalizedText,
        catalogGroupId,
        ativo,
        pageable);
      case SERVICES -> serviceItemRepository.search(
        scope.tenantId(),
        scope.catalogConfigurationId(),
        scope.agrupadorId(),
        normalizedCodigo,
        normalizedText,
        catalogGroupId,
        ativo,
        pageable);
    };

    Set<Long> groupIds = page.getContent().stream()
      .map(CatalogItemBase::getCatalogGroupId)
      .filter(id -> id != null && id > 0)
      .collect(Collectors.toSet());
    Map<Long, String> groupNameById = loadGroupNameById(scope.tenantId(), scope.catalogConfigurationId(), groupIds);

    return page.map(item -> toResponse(type, scope.agrupadorNome(), item, groupNameById));
  }

  @Transactional(readOnly = true)
  public CatalogItemResponse get(CatalogConfigurationType type, Long id) {
    var scope = contextService.resolveObrigatorio(type);
    CatalogItemBase entity = findByScope(type, id, scope);
    String groupName = resolveGroupName(scope.tenantId(), scope.catalogConfigurationId(), entity.getCatalogGroupId());
    return toResponse(type, scope.agrupadorNome(), entity, singleGroupNameMap(entity.getCatalogGroupId(), groupName));
  }

  @Transactional
  public CatalogItemResponse create(CatalogConfigurationType type, CatalogItemRequest request) {
    var scope = contextService.resolveObrigatorio(type);
    Long catalogGroupId = validateGroup(scope.tenantId(), scope.catalogConfigurationId(), request.catalogGroupId());
    CatalogItemBase entity = newEntity(type);
    entity.setTenantId(scope.tenantId());
    entity.setCatalogConfigurationId(scope.catalogConfigurationId());
    entity.setAgrupadorEmpresaId(scope.agrupadorId());
    entity.setCatalogGroupId(catalogGroupId);
    entity.setNome(normalizeNome(request.nome()));
    entity.setDescricao(normalizeDescricao(request.descricao()));
    entity.setAtivo(Boolean.TRUE.equals(request.ativo()));
    entity.setCodigo(resolveCodigoForCreate(scope, request.codigo()));

    CatalogItemBase saved = save(type, entity);
    auditService.log(
      scope.tenantId(),
      "CATALOG_ITEM_CREATED",
      entityName(type),
      String.valueOf(saved.getId()),
      "type=" + type
        + ";catalogConfigurationId=" + scope.catalogConfigurationId()
        + ";agrupadorId=" + scope.agrupadorId()
        + ";codigo=" + saved.getCodigo());

    String groupName = resolveGroupName(scope.tenantId(), scope.catalogConfigurationId(), saved.getCatalogGroupId());
    return toResponse(type, scope.agrupadorNome(), saved, singleGroupNameMap(saved.getCatalogGroupId(), groupName));
  }

  @Transactional
  public CatalogItemResponse update(CatalogConfigurationType type, Long id, CatalogItemRequest request) {
    var scope = contextService.resolveObrigatorio(type);
    CatalogItemBase entity = findByScope(type, id, scope);
    Long catalogGroupId = validateGroup(scope.tenantId(), scope.catalogConfigurationId(), request.catalogGroupId());

    entity.setCatalogGroupId(catalogGroupId);
    entity.setNome(normalizeNome(request.nome()));
    entity.setDescricao(normalizeDescricao(request.descricao()));
    entity.setAtivo(Boolean.TRUE.equals(request.ativo()));

    if (scope.numberingMode() == CatalogNumberingMode.MANUAL) {
      Long codigo = normalizeCodigo(request.codigo());
      if (codigo == null) {
        throw new IllegalArgumentException("catalog_item_codigo_required_manual");
      }
      entity.setCodigo(codigo);
    }

    CatalogItemBase saved = save(type, entity);
    auditService.log(
      scope.tenantId(),
      "CATALOG_ITEM_UPDATED",
      entityName(type),
      String.valueOf(saved.getId()),
      "type=" + type
        + ";catalogConfigurationId=" + scope.catalogConfigurationId()
        + ";agrupadorId=" + scope.agrupadorId()
        + ";codigo=" + saved.getCodigo());

    String groupName = resolveGroupName(scope.tenantId(), scope.catalogConfigurationId(), saved.getCatalogGroupId());
    return toResponse(type, scope.agrupadorNome(), saved, singleGroupNameMap(saved.getCatalogGroupId(), groupName));
  }

  @Transactional
  public void delete(CatalogConfigurationType type, Long id) {
    var scope = contextService.resolveObrigatorio(type);
    CatalogItemBase entity = findByScope(type, id, scope);
    entity.setAtivo(false);
    save(type, entity);
    auditService.log(
      scope.tenantId(),
      "CATALOG_ITEM_DELETED",
      entityName(type),
      String.valueOf(entity.getId()),
      "type=" + type
        + ";catalogConfigurationId=" + scope.catalogConfigurationId()
        + ";agrupadorId=" + scope.agrupadorId()
        + ";codigo=" + entity.getCodigo());
  }

  private CatalogItemBase save(CatalogConfigurationType type, CatalogItemBase entity) {
    try {
      return switch (type) {
        case PRODUCTS -> productRepository.save((CatalogProduct) entity);
        case SERVICES -> serviceItemRepository.save((CatalogServiceItem) entity);
      };
    } catch (DataIntegrityViolationException ex) {
      throw mapIntegrityViolation(ex);
    }
  }

  private CatalogItemBase findByScope(CatalogConfigurationType type, Long id, CatalogItemContextService.CatalogItemScope scope) {
    return switch (type) {
      case PRODUCTS -> productRepository
        .findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaId(
          id, scope.tenantId(), scope.catalogConfigurationId(), scope.agrupadorId())
        .orElseThrow(() -> new EntityNotFoundException("catalog_item_not_found"));
      case SERVICES -> serviceItemRepository
        .findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaId(
          id, scope.tenantId(), scope.catalogConfigurationId(), scope.agrupadorId())
        .orElseThrow(() -> new EntityNotFoundException("catalog_item_not_found"));
    };
  }

  private CatalogItemBase newEntity(CatalogConfigurationType type) {
    return switch (type) {
      case PRODUCTS -> new CatalogProduct();
      case SERVICES -> new CatalogServiceItem();
    };
  }

  private String entityName(CatalogConfigurationType type) {
    return type == CatalogConfigurationType.PRODUCTS ? "catalog_product" : "catalog_service_item";
  }

  private Long resolveCodigoForCreate(CatalogItemContextService.CatalogItemScope scope, Long requestedCodigo) {
    if (scope.numberingMode() == CatalogNumberingMode.AUTOMATICA) {
      return codeService.proximoCodigo(scope.tenantId(), scope.catalogConfigurationId(), scope.agrupadorId());
    }
    Long codigo = normalizeCodigo(requestedCodigo);
    if (codigo == null) {
      throw new IllegalArgumentException("catalog_item_codigo_required_manual");
    }
    return codigo;
  }

  private Long validateGroup(Long tenantId, Long catalogConfigurationId, Long catalogGroupId) {
    if (catalogGroupId == null) return null;
    CatalogGroup group = groupRepository
      .findByIdAndTenantIdAndCatalogConfigurationIdAndAtivoTrue(catalogGroupId, tenantId, catalogConfigurationId)
      .orElseThrow(() -> new EntityNotFoundException("catalog_group_not_found"));
    return group.getId();
  }

  private Map<Long, String> loadGroupNameById(Long tenantId, Long catalogConfigurationId, Set<Long> groupIds) {
    if (groupIds == null || groupIds.isEmpty()) return Map.of();
    return groupRepository.findAllByTenantIdAndCatalogConfigurationIdAndIdIn(tenantId, catalogConfigurationId, groupIds)
      .stream()
      .collect(Collectors.toMap(CatalogGroup::getId, CatalogGroup::getNome, (a, b) -> a, HashMap::new));
  }

  private String resolveGroupName(Long tenantId, Long catalogConfigurationId, Long catalogGroupId) {
    if (catalogGroupId == null) return null;
    return groupRepository.findByIdAndTenantIdAndCatalogConfigurationIdAndAtivoTrue(
        catalogGroupId, tenantId, catalogConfigurationId)
      .map(CatalogGroup::getNome)
      .orElse(null);
  }

  private CatalogItemResponse toResponse(
      CatalogConfigurationType type,
      String agrupadorNome,
      CatalogItemBase entity,
      Map<Long, String> groupNameById) {
    String groupName = entity.getCatalogGroupId() == null
      ? null
      : groupNameById.get(entity.getCatalogGroupId());
    return new CatalogItemResponse(
      entity.getId(),
      type,
      entity.getCatalogConfigurationId(),
      entity.getAgrupadorEmpresaId(),
      agrupadorNome,
      entity.getCatalogGroupId(),
      groupName,
      entity.getCodigo(),
      entity.getNome(),
      entity.getDescricao(),
      entity.isAtivo());
  }

  private Map<Long, String> singleGroupNameMap(Long groupId, String groupName) {
    if (groupId == null) return Map.of();
    Map<Long, String> map = new HashMap<>();
    map.put(groupId, groupName);
    return map;
  }

  private RuntimeException mapIntegrityViolation(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage().toLowerCase();
    if (message.contains("ux_catalog_product_codigo_scope")
      || message.contains("ux_catalog_service_item_codigo_scope")) {
      return new IllegalArgumentException("catalog_item_codigo_duplicado");
    }
    throw ex;
  }

  private Long normalizeCodigo(Long codigo) {
    if (codigo == null) return null;
    return codigo > 0 ? codigo : null;
  }

  private String normalizeText(String value) {
    if (value == null) return null;
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private String normalizeNome(String nome) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("catalog_item_nome_required");
    }
    String normalized = nome.trim();
    if (normalized.length() > 200) {
      throw new IllegalArgumentException("catalog_item_nome_too_long");
    }
    return normalized;
  }

  private String normalizeDescricao(String descricao) {
    if (descricao == null) return null;
    String normalized = descricao.trim();
    if (normalized.isEmpty()) return null;
    if (normalized.length() > 255) {
      throw new IllegalArgumentException("catalog_item_descricao_too_long");
    }
    return normalized;
  }
}
