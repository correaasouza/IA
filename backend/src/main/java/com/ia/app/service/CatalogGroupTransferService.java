package com.ia.app.service;

import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogMovementMetricType;
import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.domain.CatalogStockBalance;
import com.ia.app.domain.CatalogStockType;
import com.ia.app.repository.CatalogConfigurationRepository;
import com.ia.app.repository.CatalogStockBalanceRepository;
import com.ia.app.repository.CatalogStockTypeRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogGroupTransferService {

  private final CatalogConfigurationRepository configurationRepository;
  private final CatalogStockBalanceRepository stockBalanceRepository;
  private final CatalogStockTypeRepository stockTypeRepository;
  private final CatalogStockTypeSyncService stockTypeSyncService;
  private final CatalogMovementEngine movementEngine;
  private final AuditService auditService;

  public CatalogGroupTransferService(
      CatalogConfigurationRepository configurationRepository,
      CatalogStockBalanceRepository stockBalanceRepository,
      CatalogStockTypeRepository stockTypeRepository,
      CatalogStockTypeSyncService stockTypeSyncService,
      CatalogMovementEngine movementEngine,
      AuditService auditService) {
    this.configurationRepository = configurationRepository;
    this.stockBalanceRepository = stockBalanceRepository;
    this.stockTypeRepository = stockTypeRepository;
    this.stockTypeSyncService = stockTypeSyncService;
    this.movementEngine = movementEngine;
    this.auditService = auditService;
  }

  @Transactional
  public void transferOnEmpresaGroupChange(
      Long tenantId,
      Long catalogConfigurationId,
      Long empresaId,
      Long fromAgrupadorId,
      Long toAgrupadorId,
      String transferKey) {
    if (tenantId == null || tenantId <= 0
      || catalogConfigurationId == null || catalogConfigurationId <= 0
      || empresaId == null || empresaId <= 0
      || fromAgrupadorId == null || fromAgrupadorId <= 0
      || toAgrupadorId == null || toAgrupadorId <= 0) {
      return;
    }
    if (Objects.equals(fromAgrupadorId, toAgrupadorId)) {
      return;
    }

    CatalogConfiguration configuration = configurationRepository
      .findByIdAndTenantId(catalogConfigurationId, tenantId)
      .orElse(null);
    if (configuration == null) {
      return;
    }

    stockTypeSyncService.ensureDefaultForGroup(tenantId, catalogConfigurationId, toAgrupadorId);

    List<CatalogStockBalance> sourceBalances = stockBalanceRepository
      .findAllByTenantIdAndCatalogTypeAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndFilialId(
        tenantId,
        configuration.getType(),
        catalogConfigurationId,
        fromAgrupadorId,
        empresaId);

    if (sourceBalances.isEmpty()) {
      return;
    }

    Map<Long, CatalogStockType> sourceStockTypeById = new HashMap<>();
    List<Long> sourceStockTypeIds = sourceBalances.stream().map(CatalogStockBalance::getEstoqueTipoId).distinct().toList();
    for (CatalogStockType stockType : stockTypeRepository.findAllByTenantIdAndIdIn(tenantId, sourceStockTypeIds)) {
      sourceStockTypeById.put(stockType.getId(), stockType);
    }

    Map<String, CatalogStockType> targetByCode = new HashMap<>();
    for (CatalogStockType stockType : stockTypeRepository
      .findAllByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrueOrderByOrdemAscNomeAsc(
        tenantId, catalogConfigurationId, toAgrupadorId)) {
      targetByCode.put(stockType.getCodigo(), stockType);
    }

    String safeTransferKey = (transferKey == null || transferKey.isBlank()) ? "fallback" : transferKey.trim();

    Map<Long, List<CatalogStockBalance>> byCatalogo = sourceBalances.stream()
      .collect(Collectors.groupingBy(CatalogStockBalance::getCatalogoId));

    for (Map.Entry<Long, List<CatalogStockBalance>> entry : byCatalogo.entrySet()) {
      Long catalogoId = entry.getKey();
      List<CatalogMovementEngine.Impact> impacts = buildImpacts(
        entry.getValue(),
        sourceStockTypeById,
        targetByCode,
        tenantId,
        catalogConfigurationId,
        empresaId,
        fromAgrupadorId,
        toAgrupadorId);
      if (impacts.isEmpty()) {
        continue;
      }

      String idempotencyKey = ("catalog-group-change:"
        + catalogConfigurationId
        + ":" + catalogoId
        + ":" + empresaId
        + ":" + fromAgrupadorId
        + ":" + toAgrupadorId
        + ":" + safeTransferKey);

      CatalogMovementEngine.Command command = new CatalogMovementEngine.Command(
        tenantId,
        configuration.getType(),
        catalogoId,
        catalogConfigurationId,
        fromAgrupadorId,
        CatalogMovementOriginType.MUDANCA_GRUPO,
        "EMPRESA:" + empresaId,
        empresaId,
        "TRANSFERENCIA_GRUPO",
        "GRUPO:" + fromAgrupadorId + "->" + toAgrupadorId,
        null,
        null,
        null,
        "Transferencia automatica ao mover empresa entre agrupadores de catalogo.",
        idempotencyKey,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        impacts);

      CatalogMovementEngine.Result result = movementEngine.apply(command);
      auditService.log(
        tenantId,
        "CATALOG_STOCK_GROUP_TRANSFER",
        "catalog_stock_balance",
        String.valueOf(catalogoId),
        "catalogConfigurationId=" + catalogConfigurationId
          + ";empresaId=" + empresaId
          + ";fromAgrupadorId=" + fromAgrupadorId
          + ";toAgrupadorId=" + toAgrupadorId
          + ";movementId=" + result.movementId()
          + ";reused=" + result.reused());
    }
  }

  private List<CatalogMovementEngine.Impact> buildImpacts(
      List<CatalogStockBalance> balances,
      Map<Long, CatalogStockType> sourceStockTypeById,
      Map<String, CatalogStockType> targetByCode,
      Long tenantId,
      Long catalogConfigurationId,
      Long empresaId,
      Long fromAgrupadorId,
      Long toAgrupadorId) {
    List<CatalogMovementEngine.Impact> impacts = new ArrayList<>();
    for (CatalogStockBalance sourceBalance : balances) {
      CatalogStockType sourceStockType = sourceStockTypeById.get(sourceBalance.getEstoqueTipoId());
      if (sourceStockType == null || !sourceStockType.isActive()) {
        continue;
      }

      CatalogStockType targetStockType = targetByCode.get(sourceStockType.getCodigo());
      if (targetStockType == null) {
        targetStockType = stockTypeSyncService.ensureByCode(
          tenantId,
          catalogConfigurationId,
          toAgrupadorId,
          sourceStockType.getCodigo(),
          sourceStockType.getNome(),
          sourceStockType.getOrdem());
        targetByCode.put(targetStockType.getCodigo(), targetStockType);
      }

      addTransferImpacts(
        impacts,
        sourceBalance.getQuantidadeAtual(),
        CatalogMovementMetricType.QUANTIDADE,
        empresaId,
        fromAgrupadorId,
        toAgrupadorId,
        sourceStockType.getId(),
        targetStockType.getId());

      addTransferImpacts(
        impacts,
        sourceBalance.getPrecoAtual(),
        CatalogMovementMetricType.PRECO,
        empresaId,
        fromAgrupadorId,
        toAgrupadorId,
        sourceStockType.getId(),
        targetStockType.getId());
    }
    return impacts;
  }

  private void addTransferImpacts(
      List<CatalogMovementEngine.Impact> impacts,
      BigDecimal sourceValue,
      CatalogMovementMetricType metricType,
      Long filialId,
      Long fromAgrupadorId,
      Long toAgrupadorId,
      Long fromEstoqueTipoId,
      Long toEstoqueTipoId) {
    if (sourceValue == null || sourceValue.compareTo(BigDecimal.ZERO) == 0) {
      return;
    }

    impacts.add(new CatalogMovementEngine.Impact(
      fromAgrupadorId,
      metricType,
      fromEstoqueTipoId,
      filialId,
      sourceValue.negate()));

    impacts.add(new CatalogMovementEngine.Impact(
      toAgrupadorId,
      metricType,
      toEstoqueTipoId,
      filialId,
      sourceValue));
  }
}
