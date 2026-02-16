package com.ia.app.service;

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
import com.ia.app.repository.CatalogMovementLineRepository;
import com.ia.app.repository.CatalogMovementRepository;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.CatalogServiceItemRepository;
import com.ia.app.repository.CatalogStockBalanceRepository;
import com.ia.app.repository.CatalogStockTypeRepository;
import com.ia.app.repository.EmpresaRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
  private final CatalogStockTypeRepository stockTypeRepository;
  private final EmpresaRepository empresaRepository;

  public CatalogStockQueryService(
      CatalogItemContextService contextService,
      CatalogProductRepository productRepository,
      CatalogServiceItemRepository serviceItemRepository,
      CatalogStockBalanceRepository balanceRepository,
      CatalogMovementRepository movementRepository,
      CatalogMovementLineRepository lineRepository,
      CatalogStockTypeRepository stockTypeRepository,
      EmpresaRepository empresaRepository) {
    this.contextService = contextService;
    this.productRepository = productRepository;
    this.serviceItemRepository = serviceItemRepository;
    this.balanceRepository = balanceRepository;
    this.movementRepository = movementRepository;
    this.lineRepository = lineRepository;
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

    Map<Long, CatalogStockType> stockTypeById = loadStockTypes(scope.tenantId(), collectStockTypeIds(rows, consolidatedRows));
    Map<Long, String> filialNameById = loadFilialNames(scope.tenantId(), rows.stream()
      .map(CatalogStockBalance::getFilialId)
      .collect(Collectors.toSet()));

    List<CatalogStockBalanceRowResponse> rowResponses = rows.stream()
      .map(row -> {
        CatalogStockType stockType = stockTypeById.get(row.getEstoqueTipoId());
        return new CatalogStockBalanceRowResponse(
          row.getEstoqueTipoId(),
          stockType == null ? null : stockType.getCodigo(),
          stockType == null ? null : stockType.getNome(),
          row.getFilialId(),
          filialNameById.get(row.getFilialId()),
          row.getQuantidadeAtual(),
          row.getPrecoAtual());
      })
      .toList();

    List<CatalogStockConsolidatedResponse> consolidatedResponses = consolidatedRows.stream()
      .map(row -> {
        CatalogStockType stockType = stockTypeById.get(row.getEstoqueTipoId());
        return new CatalogStockConsolidatedResponse(
          row.getEstoqueTipoId(),
          stockType == null ? null : stockType.getCodigo(),
          stockType == null ? null : stockType.getNome(),
          row.getQuantidadeTotal(),
          row.getPrecoTotal());
      })
      .toList();

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
      List<CatalogStockBalanceRepository.StockTypeConsolidatedRow> consolidatedRows) {
    Set<Long> ids = rows.stream().map(CatalogStockBalance::getEstoqueTipoId).collect(Collectors.toSet());
    ids.addAll(consolidatedRows.stream().map(CatalogStockBalanceRepository.StockTypeConsolidatedRow::getEstoqueTipoId).toList());
    return ids;
  }

  private Map<Long, CatalogStockType> loadStockTypes(Long tenantId, Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    return stockTypeRepository.findAllByTenantIdAndIdIn(tenantId, ids).stream()
      .collect(Collectors.toMap(CatalogStockType::getId, stockType -> stockType, (a, b) -> a, HashMap::new));
  }

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
