package com.ia.app.service;

import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogConfigurationByGroup;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogPriceRuleByGroup;
import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.PriceAdjustmentKind;
import com.ia.app.domain.PriceBaseMode;
import com.ia.app.domain.PriceUiLockMode;
import com.ia.app.dto.CatalogPriceRuleBulkUpsertRequest;
import com.ia.app.dto.CatalogPriceRuleResponse;
import com.ia.app.dto.CatalogPriceRuleUpsertRequest;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.CatalogConfigurationByGroupRepository;
import com.ia.app.repository.CatalogPriceRuleByGroupRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogPriceRuleService {

  public static final int PRICE_SCALE = 6;

  private final CatalogConfigurationService catalogConfigurationService;
  private final CatalogConfigurationByGroupRepository byGroupRepository;
  private final CatalogConfigurationGroupSyncService syncService;
  private final AgrupadorEmpresaRepository agrupadorEmpresaRepository;
  private final CatalogPriceRuleByGroupRepository repository;

  public CatalogPriceRuleService(
      CatalogConfigurationService catalogConfigurationService,
      CatalogConfigurationByGroupRepository byGroupRepository,
      CatalogConfigurationGroupSyncService syncService,
      AgrupadorEmpresaRepository agrupadorEmpresaRepository,
      CatalogPriceRuleByGroupRepository repository) {
    this.catalogConfigurationService = catalogConfigurationService;
    this.byGroupRepository = byGroupRepository;
    this.syncService = syncService;
    this.agrupadorEmpresaRepository = agrupadorEmpresaRepository;
    this.repository = repository;
  }

  @Transactional
  public List<CatalogPriceRuleResponse> listByGroup(CatalogConfigurationType type, Long agrupadorId) {
    Long tenantId = requireTenant();
    CatalogConfigurationByGroup byGroup = resolveByGroupEntity(tenantId, type, agrupadorId);
    ensureDefaults(tenantId, byGroup.getId());
    return repository.findAllByTenantIdAndCatalogConfigurationByGroupIdAndActiveTrueOrderByPriceTypeAsc(tenantId, byGroup.getId())
      .stream()
      .sorted(Comparator.comparing(CatalogPriceRuleByGroup::getPriceType))
      .map(this::toResponse)
      .toList();
  }

  @Transactional
  public List<CatalogPriceRuleResponse> upsertByGroup(
      CatalogConfigurationType type,
      Long agrupadorId,
      CatalogPriceRuleBulkUpsertRequest request) {
    Long tenantId = requireTenant();
    CatalogConfigurationByGroup byGroup = resolveByGroupEntity(tenantId, type, agrupadorId);
    ensureDefaults(tenantId, byGroup.getId());

    Map<CatalogPriceType, CatalogPriceRuleByGroup> current = loadRuleMap(tenantId, byGroup.getId());
    for (CatalogPriceRuleUpsertRequest item : request.rules()) {
      CatalogPriceRuleByGroup entity = current.computeIfAbsent(item.priceType(), ignored -> defaultRule(tenantId, byGroup.getId(), item.priceType()));
      applyRequest(entity, item);
    }

    ensureAllTypesPresent(current, tenantId, byGroup.getId());
    validateRules(current);

    repository.saveAll(current.values());
    return current.values().stream()
      .sorted(Comparator.comparing(CatalogPriceRuleByGroup::getPriceType))
      .map(this::toResponse)
      .toList();
  }

  @Transactional
  public Long resolveByGroupConfigurationId(CatalogConfigurationType type, Long agrupadorId) {
    Long tenantId = requireTenant();
    CatalogConfigurationByGroup byGroup = resolveByGroupEntity(tenantId, type, agrupadorId);
    ensureDefaults(tenantId, byGroup.getId());
    return byGroup.getId();
  }

  @Transactional
  public Map<CatalogPriceType, CatalogPriceRuleByGroup> loadRuleMap(Long tenantId, Long catalogConfigurationByGroupId) {
    ensureDefaults(tenantId, catalogConfigurationByGroupId);
    Map<CatalogPriceType, CatalogPriceRuleByGroup> map = new EnumMap<>(CatalogPriceType.class);
    for (CatalogPriceRuleByGroup row : repository.findAllByTenantIdAndCatalogConfigurationByGroupIdAndActiveTrueOrderByPriceTypeAsc(
      tenantId,
      catalogConfigurationByGroupId)) {
      map.put(row.getPriceType(), row);
    }
    ensureAllTypesPresent(map, tenantId, catalogConfigurationByGroupId);
    validateRules(map);
    return map;
  }

  private void applyRequest(CatalogPriceRuleByGroup entity, CatalogPriceRuleUpsertRequest request) {
    entity.setCustomName(normalizeOptionalName(request.customName()));
    entity.setBaseMode(request.baseMode());
    entity.setBasePriceType(request.basePriceType());
    entity.setAdjustmentKindDefault(request.adjustmentKindDefault());
    entity.setAdjustmentDefault(normalizeScale(request.adjustmentDefault()));
    entity.setUiLockMode(request.uiLockMode());
    entity.setActive(Boolean.TRUE.equals(request.active()));
  }

  private CatalogConfigurationByGroup resolveByGroupEntity(Long tenantId, CatalogConfigurationType type, Long agrupadorId) {
    if (agrupadorId == null || agrupadorId <= 0) {
      throw new IllegalArgumentException("agrupador_id_invalid");
    }

    CatalogConfiguration config = catalogConfigurationService.getEntityOrCreate(type);
    AgrupadorEmpresa agrupador = agrupadorEmpresaRepository
      .findByIdAndTenantIdAndConfigTypeAndConfigIdAndAtivoTrue(
        agrupadorId,
        tenantId,
        ConfiguracaoScopeService.TYPE_CATALOGO,
        config.getId())
      .orElseThrow(() -> new EntityNotFoundException("agrupador_not_found"));

    syncService.onAgrupadorCreated(tenantId, config.getId(), agrupador.getId());

    return byGroupRepository.findByTenantIdAndCatalogConfigurationIdAndAgrupadorIdAndActiveTrue(tenantId, config.getId(), agrupadorId)
      .orElseThrow(() -> new EntityNotFoundException("catalog_configuration_group_not_found"));
  }

  private void ensureDefaults(Long tenantId, Long catalogConfigurationByGroupId) {
    Map<CatalogPriceType, CatalogPriceRuleByGroup> map = new EnumMap<>(CatalogPriceType.class);
    for (CatalogPriceRuleByGroup row : repository.findAllByTenantIdAndCatalogConfigurationByGroupIdAndActiveTrueOrderByPriceTypeAsc(
      tenantId,
      catalogConfigurationByGroupId)) {
      map.put(row.getPriceType(), row);
    }

    List<CatalogPriceRuleByGroup> toSave = new ArrayList<>();
    for (CatalogPriceType type : CatalogPriceType.values()) {
      if (!map.containsKey(type)) {
        toSave.add(defaultRule(tenantId, catalogConfigurationByGroupId, type));
      }
    }
    if (!toSave.isEmpty()) {
      repository.saveAll(toSave);
    }
  }

  private void ensureAllTypesPresent(
      Map<CatalogPriceType, CatalogPriceRuleByGroup> map,
      Long tenantId,
      Long catalogConfigurationByGroupId) {
    for (CatalogPriceType type : CatalogPriceType.values()) {
      map.computeIfAbsent(type, ignored -> defaultRule(tenantId, catalogConfigurationByGroupId, type));
    }
  }

  private CatalogPriceRuleByGroup defaultRule(Long tenantId, Long byGroupId, CatalogPriceType type) {
    CatalogPriceRuleByGroup row = new CatalogPriceRuleByGroup();
    row.setTenantId(tenantId);
    row.setCatalogConfigurationByGroupId(byGroupId);
    row.setPriceType(type);
    row.setAdjustmentKindDefault(PriceAdjustmentKind.FIXED);
    row.setAdjustmentDefault(BigDecimal.ZERO.setScale(PRICE_SCALE, RoundingMode.HALF_UP));
    row.setActive(true);

    switch (type) {
      case PURCHASE -> {
        row.setBaseMode(PriceBaseMode.NONE);
        row.setBasePriceType(null);
        row.setUiLockMode(PriceUiLockMode.II);
      }
      case COST -> {
        row.setBaseMode(PriceBaseMode.BASE_PRICE);
        row.setBasePriceType(CatalogPriceType.PURCHASE);
        row.setUiLockMode(PriceUiLockMode.IV);
      }
      case AVERAGE_COST -> {
        row.setBaseMode(PriceBaseMode.BASE_PRICE);
        row.setBasePriceType(CatalogPriceType.COST);
        row.setUiLockMode(PriceUiLockMode.IV);
      }
      case SALE_BASE -> {
        row.setBaseMode(PriceBaseMode.BASE_PRICE);
        row.setBasePriceType(CatalogPriceType.AVERAGE_COST);
        row.setUiLockMode(PriceUiLockMode.IV);
      }
      default -> throw new IllegalArgumentException("catalog_price_type_invalid");
    }
    return row;
  }

  private void validateRules(Map<CatalogPriceType, CatalogPriceRuleByGroup> map) {
    int noneRoots = 0;
    for (CatalogPriceType type : CatalogPriceType.values()) {
      CatalogPriceRuleByGroup row = map.get(type);
      if (row == null) {
        throw new IllegalArgumentException("catalog_price_rule_missing_type");
      }

      if (row.getBaseMode() == PriceBaseMode.NONE) {
        noneRoots++;
        if (row.getBasePriceType() != null) {
          throw new IllegalArgumentException("catalog_price_rule_base_none_invalid");
        }
        if (row.getUiLockMode() != PriceUiLockMode.II) {
          throw new IllegalArgumentException("catalog_price_rule_none_requires_mode_ii");
        }
      } else {
        if (row.getBasePriceType() == null) {
          throw new IllegalArgumentException("catalog_price_rule_base_required");
        }
        if (row.getBasePriceType() == type) {
          throw new IllegalArgumentException("catalog_price_rule_self_reference");
        }
      }
    }

    if (noneRoots == 0) {
      throw new IllegalArgumentException("catalog_price_rule_none_root_required");
    }

    validateCycle(map);
  }

  private void validateCycle(Map<CatalogPriceType, CatalogPriceRuleByGroup> map) {
    Set<CatalogPriceType> visiting = new HashSet<>();
    Set<CatalogPriceType> visited = new HashSet<>();

    for (CatalogPriceType type : CatalogPriceType.values()) {
      if (!visited.contains(type) && hasCycle(type, map, visiting, visited)) {
        throw new IllegalArgumentException("catalog_price_rule_cycle_detected");
      }
    }
  }

  private boolean hasCycle(
      CatalogPriceType node,
      Map<CatalogPriceType, CatalogPriceRuleByGroup> map,
      Set<CatalogPriceType> visiting,
      Set<CatalogPriceType> visited) {
    if (visiting.contains(node)) {
      return true;
    }
    if (visited.contains(node)) {
      return false;
    }

    visiting.add(node);
    CatalogPriceRuleByGroup row = map.get(node);
    if (row != null && row.getBaseMode() == PriceBaseMode.BASE_PRICE && row.getBasePriceType() != null) {
      if (hasCycle(row.getBasePriceType(), map, visiting, visited)) {
        return true;
      }
    }
    visiting.remove(node);
    visited.add(node);
    return false;
  }

  public List<CatalogPriceType> topologicalOrder(Map<CatalogPriceType, CatalogPriceRuleByGroup> rules) {
    Map<CatalogPriceType, Integer> indegree = new EnumMap<>(CatalogPriceType.class);
    Map<CatalogPriceType, List<CatalogPriceType>> reverseEdges = new EnumMap<>(CatalogPriceType.class);

    for (CatalogPriceType type : CatalogPriceType.values()) {
      indegree.put(type, 0);
      reverseEdges.put(type, new ArrayList<>());
    }

    for (CatalogPriceType type : CatalogPriceType.values()) {
      CatalogPriceRuleByGroup row = rules.get(type);
      if (row != null && row.getBaseMode() == PriceBaseMode.BASE_PRICE && row.getBasePriceType() != null) {
        indegree.put(type, indegree.get(type) + 1);
        reverseEdges.get(row.getBasePriceType()).add(type);
      }
    }

    ArrayDeque<CatalogPriceType> queue = new ArrayDeque<>();
    for (CatalogPriceType type : CatalogPriceType.values()) {
      if (indegree.get(type) == 0) {
        queue.add(type);
      }
    }

    List<CatalogPriceType> order = new ArrayList<>(CatalogPriceType.values().length);
    while (!queue.isEmpty()) {
      CatalogPriceType current = queue.removeFirst();
      order.add(current);
      for (CatalogPriceType dependent : reverseEdges.get(current)) {
        int next = indegree.get(dependent) - 1;
        indegree.put(dependent, next);
        if (next == 0) {
          queue.add(dependent);
        }
      }
    }

    if (order.size() != CatalogPriceType.values().length) {
      throw new IllegalArgumentException("catalog_price_rule_cycle_detected");
    }

    return order;
  }

  private CatalogPriceRuleResponse toResponse(CatalogPriceRuleByGroup entity) {
    return new CatalogPriceRuleResponse(
      entity.getId(),
      entity.getPriceType(),
      entity.getCustomName(),
      entity.getBaseMode(),
      entity.getBasePriceType(),
      entity.getAdjustmentKindDefault(),
      normalizeScale(entity.getAdjustmentDefault()),
      entity.getUiLockMode(),
      entity.isActive());
  }

  private BigDecimal normalizeScale(BigDecimal value) {
    BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
    return resolved.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
  }

  private String normalizeOptionalName(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    if (normalized.length() > 80) {
      throw new IllegalArgumentException("catalog_price_rule_custom_name_too_long");
    }
    return normalized;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
