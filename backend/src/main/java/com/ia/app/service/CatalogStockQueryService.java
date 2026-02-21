package com.ia.app.service;

import com.ia.app.domain.AgrupadorEmpresaItem;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogMovement;
import com.ia.app.domain.CatalogMovementLine;
import com.ia.app.domain.CatalogMovementMetricType;
import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.domain.CatalogProduct;
import com.ia.app.domain.CatalogServiceItem;
import com.ia.app.domain.CatalogStockBalance;
import com.ia.app.domain.CatalogStockType;
import com.ia.app.domain.Empresa;
import com.ia.app.dto.CatalogMovementLineResponse;
import com.ia.app.dto.CatalogMovementResponse;
import com.ia.app.dto.CatalogStockBalanceRowResponse;
import com.ia.app.dto.CatalogStockBalanceViewResponse;
import com.ia.app.dto.CatalogStockConsolidatedResponse;
import com.ia.app.repository.AgrupadorEmpresaItemRepository;
import com.ia.app.repository.CatalogMovementLineRepository;
import com.ia.app.repository.CatalogMovementRepository;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.CatalogServiceItemRepository;
import com.ia.app.repository.CatalogStockBalanceRepository;
import com.ia.app.repository.CatalogStockTypeRepository;
import com.ia.app.repository.EmpresaRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogStockQueryService {

  private static final Instant MIN_LEDGER_DATE = Instant.parse("1900-01-01T00:00:00Z");
  private static final Instant MAX_LEDGER_DATE = Instant.parse("2999-12-31T23:59:59Z");

  private final CatalogItemContextService contextService;
  private final CatalogProductRepository productRepository;
  private final CatalogServiceItemRepository serviceItemRepository;
  private final CatalogStockBalanceRepository balanceRepository;
  private final CatalogMovementRepository movementRepository;
  private final CatalogMovementLineRepository lineRepository;
  private final AgrupadorEmpresaItemRepository agrupadorEmpresaItemRepository;
  private final CatalogStockTypeRepository stockTypeRepository;
  private final EmpresaRepository empresaRepository;

  public CatalogStockQueryService(
      CatalogItemContextService contextService,
      CatalogProductRepository productRepository,
      CatalogServiceItemRepository serviceItemRepository,
      CatalogStockBalanceRepository balanceRepository,
      CatalogMovementRepository movementRepository,
      CatalogMovementLineRepository lineRepository,
      AgrupadorEmpresaItemRepository agrupadorEmpresaItemRepository,
      CatalogStockTypeRepository stockTypeRepository,
      EmpresaRepository empresaRepository) {
    this.contextService = contextService;
    this.productRepository = productRepository;
    this.serviceItemRepository = serviceItemRepository;
    this.balanceRepository = balanceRepository;
    this.movementRepository = movementRepository;
    this.lineRepository = lineRepository;
    this.agrupadorEmpresaItemRepository = agrupadorEmpresaItemRepository;
    this.stockTypeRepository = stockTypeRepository;
    this.empresaRepository = empresaRepository;
  }

  @Transactional(readOnly = true)
  public CatalogStockBalanceViewResponse loadBalanceView(
      CatalogConfigurationType type,
      Long catalogoId,
      Long agrupadorEmpresaId,
      Long estoqueTipoId,
      Long filialId) {
    CatalogItemContextService.CatalogItemScope scope = contextService.resolveObrigatorio(type);
    validateCatalogItem(scope, catalogoId);

    Long effectiveAgrupadorId = normalizeAgrupador(scope, agrupadorEmpresaId);

    List<CatalogStockBalance> rows = balanceRepository.listByFilters(
      scope.tenantId(),
      type,
      catalogoId,
      effectiveAgrupadorId,
      estoqueTipoId,
      filialId);

    List<CatalogStockBalanceRepository.StockTypeConsolidatedRow> consolidatedRows = balanceRepository.consolidatedByStockType(
      scope.tenantId(),
      type,
      catalogoId,
      effectiveAgrupadorId,
      estoqueTipoId);

    List<CatalogStockType> configuredStockTypes = loadConfiguredStockTypes(
      scope.tenantId(),
      scope.catalogConfigurationId(),
      effectiveAgrupadorId,
      estoqueTipoId);
    Set<Long> groupedFilialIds = loadGroupedFilialIds(
      scope.tenantId(),
      effectiveAgrupadorId,
      filialId);

    Map<Long, CatalogStockType> stockTypeById = loadStockTypes(
      scope.tenantId(),
      collectStockTypeIds(rows, consolidatedRows, configuredStockTypes));
    Set<Long> detailFilialIds = new LinkedHashSet<>(groupedFilialIds);
    detailFilialIds.addAll(rows.stream().map(CatalogStockBalance::getFilialId).toList());
    Map<Long, String> filialNameById = loadFilialNames(scope.tenantId(), detailFilialIds);

    List<CatalogStockBalanceRowResponse> rowResponses = buildDetailRows(
      rows,
      configuredStockTypes,
      groupedFilialIds,
      stockTypeById,
      filialNameById);

    List<CatalogStockConsolidatedResponse> consolidatedResponses = buildConsolidatedResponses(
      consolidatedRows,
      configuredStockTypes,
      stockTypeById);

    return new CatalogStockBalanceViewResponse(
      catalogoId,
      effectiveAgrupadorId,
      rowResponses,
      consolidatedResponses);
  }

  @Transactional(readOnly = true)
  public Page<CatalogMovementResponse> loadLedger(
      CatalogConfigurationType type,
      Long catalogoId,
      Long agrupadorEmpresaId,
      CatalogMovementOriginType origemTipo,
      Instant fromDate,
      Instant toDate,
      CatalogMovementMetricType metricType,
      Long estoqueTipoId,
      Long filialId,
      Pageable pageable) {
    CatalogItemContextService.CatalogItemScope scope = contextService.resolveObrigatorio(type);
    validateCatalogItem(scope, catalogoId);

    Long effectiveAgrupadorId = normalizeAgrupador(scope, agrupadorEmpresaId);
    Instant effectiveFromDate = fromDate == null ? MIN_LEDGER_DATE : fromDate;
    Instant effectiveToDate = toDate == null ? MAX_LEDGER_DATE : toDate;

    Page<CatalogMovement> page = movementRepository.search(
      scope.tenantId(),
      type,
      catalogoId,
      effectiveAgrupadorId,
      origemTipo,
      effectiveFromDate,
      effectiveToDate,
      metricType,
      estoqueTipoId,
      filialId,
      pageable);

    if (page.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    List<Long> movementIds = page.getContent().stream().map(CatalogMovement::getId).toList();
    List<CatalogMovementLine> lines = lineRepository.findAllByTenantIdAndMovementIdInOrderByMovementIdAscIdAsc(
      scope.tenantId(),
      movementIds);

    Map<Long, List<CatalogMovementLine>> linesByMovementId = new LinkedHashMap<>();
    Set<Long> stockTypeIds = lines.stream()
      .map(CatalogMovementLine::getEstoqueTipoId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    Set<Long> filialIds = lines.stream()
      .map(CatalogMovementLine::getFilialId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());

    for (CatalogMovementLine line : lines) {
      linesByMovementId.computeIfAbsent(line.getMovementId(), ignored -> new ArrayList<>()).add(line);
    }

    Map<Long, CatalogStockType> stockTypeById = loadStockTypes(scope.tenantId(), stockTypeIds);
    Map<Long, String> filialNameById = loadFilialNames(scope.tenantId(), filialIds);

    return page.map(movement -> {
      List<CatalogMovementLineResponse> lineResponses = linesByMovementId
        .getOrDefault(movement.getId(), List.of())
        .stream()
        .map(line -> {
          CatalogStockType stockType = stockTypeById.get(line.getEstoqueTipoId());
          return new CatalogMovementLineResponse(
            line.getId(),
            line.getAgrupadorEmpresaId(),
            line.getEstoqueTipoId(),
            stockType == null ? null : stockType.getCodigo(),
            stockType == null ? null : stockType.getNome(),
            line.getFilialId(),
            filialNameById.get(line.getFilialId()),
            line.getMetricType(),
            line.getBeforeValue(),
            line.getDelta(),
            line.getAfterValue());
        })
        .toList();

      return new CatalogMovementResponse(
        movement.getId(),
        movement.getCatalogoId(),
        movement.getAgrupadorEmpresaId(),
        movement.getOrigemMovimentacaoTipo(),
        movement.getOrigemMovimentacaoCodigo(),
        movement.getOrigemMovimentoItemCodigo(),
        movement.getDataHoraMovimentacao(),
        movement.getObservacao(),
        movement.getTenantUnitId(),
        movement.getUnidadeBaseCatalogoTenantUnitId(),
        movement.getQuantidadeInformada(),
        movement.getQuantidadeConvertidaBase(),
        movement.getFatorAplicado(),
        movement.getFatorFonte(),
        lineResponses);
    });
  }

  private void validateCatalogItem(CatalogItemContextService.CatalogItemScope scope, Long catalogoId) {
    if (catalogoId == null || catalogoId <= 0) {
      throw new EntityNotFoundException("catalog_item_not_found");
    }

    boolean valid = switch (scope.type()) {
      case PRODUCTS -> productRepository.findByIdAndTenantId(catalogoId, scope.tenantId())
        .map(CatalogProduct::getCatalogConfigurationId)
        .filter(configId -> configId.equals(scope.catalogConfigurationId()))
        .isPresent();
      case SERVICES -> serviceItemRepository.findByIdAndTenantId(catalogoId, scope.tenantId())
        .map(CatalogServiceItem::getCatalogConfigurationId)
        .filter(configId -> configId.equals(scope.catalogConfigurationId()))
        .isPresent();
    };

    if (!valid) {
      throw new EntityNotFoundException("catalog_item_not_found");
    }
  }

  private Long normalizeAgrupador(CatalogItemContextService.CatalogItemScope scope, Long agrupadorEmpresaId) {
    Long value = agrupadorEmpresaId;
    if (value == null) {
      value = scope.agrupadorId();
    }
    if (value == null || value <= 0) {
      throw new IllegalArgumentException("catalog_context_sem_grupo");
    }
    return value;
  }

  private Set<Long> collectStockTypeIds(
      List<CatalogStockBalance> rows,
      List<CatalogStockBalanceRepository.StockTypeConsolidatedRow> consolidatedRows,
      List<CatalogStockType> configuredStockTypes) {
    Set<Long> ids = rows.stream().map(CatalogStockBalance::getEstoqueTipoId).collect(Collectors.toSet());
    ids.addAll(consolidatedRows.stream().map(CatalogStockBalanceRepository.StockTypeConsolidatedRow::getEstoqueTipoId).toList());
    ids.addAll(configuredStockTypes.stream().map(CatalogStockType::getId).toList());
    return ids;
  }

  private Map<Long, CatalogStockType> loadStockTypes(Long tenantId, Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    return stockTypeRepository.findAllByTenantIdAndIdIn(tenantId, ids).stream()
      .collect(Collectors.toMap(CatalogStockType::getId, stockType -> stockType, (a, b) -> a, HashMap::new));
  }

  private List<CatalogStockType> loadConfiguredStockTypes(
      Long tenantId,
      Long catalogConfigurationId,
      Long agrupadorEmpresaId,
      Long estoqueTipoId) {
    List<CatalogStockType> configured = stockTypeRepository
      .findAllByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrueOrderByOrdemAscNomeAsc(
        tenantId,
        catalogConfigurationId,
        agrupadorEmpresaId);
    if (estoqueTipoId == null) {
      return configured;
    }
    return configured.stream()
      .filter(stockType -> Objects.equals(stockType.getId(), estoqueTipoId))
      .toList();
  }

  private List<CatalogStockConsolidatedResponse> buildConsolidatedResponses(
      List<CatalogStockBalanceRepository.StockTypeConsolidatedRow> consolidatedRows,
      List<CatalogStockType> configuredStockTypes,
      Map<Long, CatalogStockType> stockTypeById) {
    Map<Long, CatalogStockBalanceRepository.StockTypeConsolidatedRow> consolidatedByStockTypeId = new LinkedHashMap<>();
    for (CatalogStockBalanceRepository.StockTypeConsolidatedRow row : consolidatedRows) {
      if (row == null || row.getEstoqueTipoId() == null) {
        continue;
      }
      consolidatedByStockTypeId.putIfAbsent(row.getEstoqueTipoId(), row);
    }

    List<CatalogStockConsolidatedResponse> response = new ArrayList<>();
    Set<Long> includedStockTypeIds = new LinkedHashSet<>();

    for (CatalogStockType configured : configuredStockTypes) {
      CatalogStockBalanceRepository.StockTypeConsolidatedRow consolidated = consolidatedByStockTypeId.get(configured.getId());
      response.add(new CatalogStockConsolidatedResponse(
        configured.getId(),
        configured.getCodigo(),
        configured.getNome(),
        consolidated == null ? BigDecimal.ZERO : normalizeAmount(consolidated.getQuantidadeTotal()),
        consolidated == null ? BigDecimal.ZERO : normalizeAmount(consolidated.getPrecoTotal())));
      includedStockTypeIds.add(configured.getId());
    }

    for (CatalogStockBalanceRepository.StockTypeConsolidatedRow row : consolidatedRows) {
      Long stockTypeId = row == null ? null : row.getEstoqueTipoId();
      if (stockTypeId == null || includedStockTypeIds.contains(stockTypeId)) {
        continue;
      }
      CatalogStockType stockType = stockTypeById.get(stockTypeId);
      response.add(new CatalogStockConsolidatedResponse(
        stockTypeId,
        stockType == null ? null : stockType.getCodigo(),
        stockType == null ? null : stockType.getNome(),
        normalizeAmount(row.getQuantidadeTotal()),
        normalizeAmount(row.getPrecoTotal())));
    }

    return response;
  }

  private List<CatalogStockBalanceRowResponse> buildDetailRows(
      List<CatalogStockBalance> rows,
      List<CatalogStockType> configuredStockTypes,
      Collection<Long> groupedFilialIds,
      Map<Long, CatalogStockType> stockTypeById,
      Map<Long, String> filialNameById) {
    Map<DetailRowKey, CatalogStockBalance> rowByKey = new LinkedHashMap<>();
    for (CatalogStockBalance row : rows) {
      if (row == null || row.getEstoqueTipoId() == null || row.getFilialId() == null) {
        continue;
      }
      rowByKey.putIfAbsent(new DetailRowKey(row.getEstoqueTipoId(), row.getFilialId()), row);
    }

    List<CatalogStockBalanceRowResponse> response = new ArrayList<>();
    Set<DetailRowKey> includedKeys = new LinkedHashSet<>();

    for (CatalogStockType configuredStockType : configuredStockTypes) {
      for (Long filialItemId : groupedFilialIds) {
        DetailRowKey key = new DetailRowKey(configuredStockType.getId(), filialItemId);
        CatalogStockBalance row = rowByKey.get(key);
        response.add(new CatalogStockBalanceRowResponse(
          configuredStockType.getId(),
          configuredStockType.getCodigo(),
          configuredStockType.getNome(),
          filialItemId,
          filialNameById.get(filialItemId),
          row == null ? BigDecimal.ZERO : normalizeAmount(row.getQuantidadeAtual()),
          row == null ? BigDecimal.ZERO : normalizeAmount(row.getPrecoAtual())));
        includedKeys.add(key);
      }
    }

    for (CatalogStockBalance row : rows) {
      if (row == null || row.getEstoqueTipoId() == null || row.getFilialId() == null) {
        continue;
      }
      DetailRowKey key = new DetailRowKey(row.getEstoqueTipoId(), row.getFilialId());
      if (includedKeys.contains(key)) {
        continue;
      }
      CatalogStockType stockType = stockTypeById.get(row.getEstoqueTipoId());
      response.add(new CatalogStockBalanceRowResponse(
        row.getEstoqueTipoId(),
        stockType == null ? null : stockType.getCodigo(),
        stockType == null ? null : stockType.getNome(),
        row.getFilialId(),
        filialNameById.get(row.getFilialId()),
        normalizeAmount(row.getQuantidadeAtual()),
        normalizeAmount(row.getPrecoAtual())));
      includedKeys.add(key);
    }

    return response;
  }

  private Set<Long> loadGroupedFilialIds(
      Long tenantId,
      Long agrupadorEmpresaId,
      Long filialId) {
    return agrupadorEmpresaItemRepository
      .findAllByTenantIdAndAgrupadorIdOrderByEmpresaIdAsc(
        tenantId,
        agrupadorEmpresaId)
      .stream()
      .map(AgrupadorEmpresaItem::getEmpresaId)
      .filter(Objects::nonNull)
      .filter(empresaId -> filialId == null || Objects.equals(empresaId, filialId))
      .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private BigDecimal normalizeAmount(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private record DetailRowKey(Long estoqueTipoId, Long filialId) {}

  private Map<Long, String> loadFilialNames(Long tenantId, Collection<Long> filialIds) {
    if (filialIds == null || filialIds.isEmpty()) {
      return Map.of();
    }
    return empresaRepository.findAllByTenantIdAndIdIn(tenantId, filialIds).stream()
      .collect(Collectors.toMap(Empresa::getId, this::resolveEmpresaName, (a, b) -> a, HashMap::new));
  }

  private String resolveEmpresaName(Empresa empresa) {
    if (empresa == null) {
      return null;
    }
    String nomeFantasia = empresa.getNomeFantasia();
    if (nomeFantasia != null && !nomeFantasia.isBlank()) {
      return nomeFantasia;
    }
    return empresa.getRazaoSocial();
  }
}
