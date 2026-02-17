package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogMovement;
import com.ia.app.domain.CatalogMovementLine;
import com.ia.app.domain.CatalogMovementMetricType;
import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.domain.CatalogStockBalance;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.repository.CatalogMovementLineRepository;
import com.ia.app.repository.CatalogMovementRepository;
import com.ia.app.repository.CatalogStockBalanceRepository;
import com.ia.app.repository.CatalogStockTypeRepository;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.MovimentoConfigRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogMovementEngine {

  public record Impact(
    Long agrupadorEmpresaId,
    CatalogMovementMetricType metricType,
    Long estoqueTipoId,
    Long filialId,
    BigDecimal delta
  ) {}

  public record Command(
    Long tenantId,
    CatalogConfigurationType catalogType,
    Long catalogoId,
    Long catalogConfigurationId,
    Long agrupadorEmpresaId,
    CatalogMovementOriginType origemTipo,
    String origemCodigo,
    String origemItemCodigo,
    String observacao,
    String idempotencyKey,
    Instant dataHoraMovimentacao,
    List<Impact> impacts
  ) {}

  public record Result(Long movementId, boolean reused) {}

  private record BalanceKey(Long agrupadorEmpresaId, Long estoqueTipoId, Long filialId) {}

  private final CatalogMovementRepository movementRepository;
  private final CatalogMovementLineRepository lineRepository;
  private final CatalogStockBalanceRepository balanceRepository;
  private final CatalogStockTypeRepository stockTypeRepository;
  private final EmpresaRepository empresaRepository;
  private final MovimentoConfigRepository movimentoConfigRepository;

  @Value("${movimento.config.enabled:true}")
  private boolean movimentoConfigEnabled;

  @Value("${movimento.config.strict-enabled:false}")
  private boolean movimentoConfigStrictEnabled;

  public CatalogMovementEngine(
      CatalogMovementRepository movementRepository,
      CatalogMovementLineRepository lineRepository,
      CatalogStockBalanceRepository balanceRepository,
      CatalogStockTypeRepository stockTypeRepository,
      EmpresaRepository empresaRepository,
      MovimentoConfigRepository movimentoConfigRepository) {
    this.movementRepository = movementRepository;
    this.lineRepository = lineRepository;
    this.balanceRepository = balanceRepository;
    this.stockTypeRepository = stockTypeRepository;
    this.empresaRepository = empresaRepository;
    this.movimentoConfigRepository = movimentoConfigRepository;
  }

  @Transactional
  public Result apply(Command command) {
    Command normalized = normalizeCommand(command);

    Optional<CatalogMovement> existing = movementRepository.findByTenantIdAndIdempotencyKey(
      normalized.tenantId(),
      normalized.idempotencyKey());
    if (existing.isPresent()) {
      return new Result(existing.get().getId(), true);
    }

    validateStrictMovimentoConfigCoverage(normalized);

    CatalogMovement movement = buildMovementHeader(normalized);
    try {
      movement = movementRepository.saveAndFlush(movement);
    } catch (DataIntegrityViolationException ex) {
      Optional<CatalogMovement> duplicated = movementRepository.findByTenantIdAndIdempotencyKey(
        normalized.tenantId(),
        normalized.idempotencyKey());
      if (duplicated.isPresent()) {
        return new Result(duplicated.get().getId(), true);
      }
      throw ex;
    }

    Map<BalanceKey, CatalogStockBalance> balances = new LinkedHashMap<>();
    List<CatalogMovementLine> lines = new ArrayList<>();
    Map<String, Boolean> stockTypeCache = new HashMap<>();
    Map<Long, Boolean> filialCache = new HashMap<>();

    List<Impact> orderedImpacts = normalized.impacts().stream()
      .sorted(Comparator
        .comparing(Impact::agrupadorEmpresaId)
        .thenComparing(Impact::estoqueTipoId)
        .thenComparing(Impact::filialId)
        .thenComparing(impact -> impact.metricType().name()))
      .toList();

    for (Impact impact : orderedImpacts) {
      validateImpact(normalized, impact, stockTypeCache, filialCache);

      BalanceKey key = new BalanceKey(impact.agrupadorEmpresaId(), impact.estoqueTipoId(), impact.filialId());
      CatalogStockBalance balance = balances.computeIfAbsent(key, ignored -> findOrCreateBalanceWithLock(normalized, key));

      BigDecimal before = valueForMetric(balance, impact.metricType());
      BigDecimal after = before.add(impact.delta());
      applyMetric(balance, impact.metricType(), after);

      CatalogMovementLine line = new CatalogMovementLine();
      line.setMovementId(movement.getId());
      line.setTenantId(normalized.tenantId());
      line.setAgrupadorEmpresaId(impact.agrupadorEmpresaId());
      line.setMetricType(impact.metricType());
      line.setEstoqueTipoId(impact.estoqueTipoId());
      line.setFilialId(impact.filialId());
      line.setBeforeValue(before);
      line.setDelta(impact.delta());
      line.setAfterValue(after);
      lines.add(line);
    }

    if (!lines.isEmpty()) {
      lineRepository.saveAll(lines);
    }
    if (!balances.isEmpty()) {
      balanceRepository.saveAll(balances.values());
    }

    return new Result(movement.getId(), false);
  }

  private void validateStrictMovimentoConfigCoverage(Command command) {
    if (!movimentoConfigEnabled || !movimentoConfigStrictEnabled) {
      return;
    }
    Set<Long> empresaIds = command.impacts().stream()
      .map(Impact::filialId)
      .filter(value -> value != null && value > 0)
      .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    for (Long empresaId : empresaIds) {
      boolean covered = movimentoConfigRepository.existsActiveGlobalByTenantAndTipoAndEmpresa(
        command.tenantId(),
        MovimentoTipo.MOVIMENTO_ESTOQUE,
        empresaId);
      if (!covered) {
        throw new IllegalArgumentException("movimento_config_nao_encontrada");
      }
    }
  }

  private Command normalizeCommand(Command raw) {
    if (raw == null) {
      throw new IllegalArgumentException("catalog_stock_command_required");
    }
    if (raw.tenantId() == null || raw.tenantId() <= 0) {
      throw new IllegalArgumentException("tenant_required");
    }
    if (raw.catalogType() == null) {
      throw new IllegalArgumentException("catalog_configuration_type_invalid");
    }
    if (raw.catalogoId() == null || raw.catalogoId() <= 0) {
      throw new IllegalArgumentException("catalog_item_not_found");
    }
    if (raw.catalogConfigurationId() == null || raw.catalogConfigurationId() <= 0) {
      throw new IllegalArgumentException("catalog_context_required");
    }
    if (raw.agrupadorEmpresaId() == null || raw.agrupadorEmpresaId() <= 0) {
      throw new IllegalArgumentException("catalog_context_sem_grupo");
    }
    if (raw.origemTipo() == null) {
      throw new IllegalArgumentException("catalog_stock_origin_invalid");
    }

    String idempotencyKey = normalizeIdempotencyKey(raw.idempotencyKey());
    List<Impact> impacts = normalizeImpacts(raw.impacts());

    return new Command(
      raw.tenantId(),
      raw.catalogType(),
      raw.catalogoId(),
      raw.catalogConfigurationId(),
      raw.agrupadorEmpresaId(),
      raw.origemTipo(),
      normalizeOptional(raw.origemCodigo(), 120),
      normalizeOptional(raw.origemItemCodigo(), 120),
      normalizeOptional(raw.observacao(), 255),
      idempotencyKey,
      raw.dataHoraMovimentacao() == null ? Instant.now() : raw.dataHoraMovimentacao(),
      impacts);
  }

  private List<Impact> normalizeImpacts(List<Impact> rawImpacts) {
    List<Impact> normalized = (rawImpacts == null ? List.<Impact>of() : rawImpacts).stream()
      .filter(item -> item != null)
      .map(this::normalizeImpact)
      .filter(item -> item.delta().compareTo(BigDecimal.ZERO) != 0)
      .toList();

    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("catalog_stock_impact_required");
    }
    return normalized;
  }

  private Impact normalizeImpact(Impact raw) {
    if (raw.agrupadorEmpresaId() == null || raw.agrupadorEmpresaId() <= 0) {
      throw new IllegalArgumentException("agrupador_id_invalid");
    }
    if (raw.metricType() == null) {
      throw new IllegalArgumentException("catalog_stock_metric_invalid");
    }
    if (raw.estoqueTipoId() == null || raw.estoqueTipoId() <= 0) {
      throw new IllegalArgumentException("catalog_stock_type_not_found");
    }
    if (raw.filialId() == null || raw.filialId() <= 0) {
      throw new IllegalArgumentException("catalog_stock_filial_not_found");
    }
    BigDecimal delta = raw.delta();
    if (delta == null) {
      throw new IllegalArgumentException("catalog_stock_delta_required");
    }

    return new Impact(
      raw.agrupadorEmpresaId(),
      raw.metricType(),
      raw.estoqueTipoId(),
      raw.filialId(),
      delta.setScale(6, java.math.RoundingMode.HALF_UP));
  }

  private String normalizeIdempotencyKey(String value) {
    String normalized = normalizeOptional(value, 180);
    if (normalized == null || normalized.isBlank()) {
      throw new IllegalArgumentException("catalog_stock_idempotency_required");
    }
    return normalized;
  }

  private String normalizeOptional(String value, int maxLen) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    return normalized.length() > maxLen ? normalized.substring(0, maxLen) : normalized;
  }

  private CatalogMovement buildMovementHeader(Command command) {
    CatalogMovement movement = new CatalogMovement();
    movement.setTenantId(command.tenantId());
    movement.setCatalogoId(command.catalogoId());
    movement.setCatalogType(command.catalogType());
    movement.setCatalogConfigurationId(command.catalogConfigurationId());
    movement.setAgrupadorEmpresaId(command.agrupadorEmpresaId());
    movement.setOrigemMovimentacaoTipo(command.origemTipo());
    movement.setOrigemMovimentacaoCodigo(command.origemCodigo());
    movement.setOrigemMovimentoItemCodigo(command.origemItemCodigo());
    movement.setDataHoraMovimentacao(command.dataHoraMovimentacao());
    movement.setObservacao(command.observacao());
    movement.setIdempotencyKey(command.idempotencyKey());
    return movement;
  }

  private void validateImpact(
      Command command,
      Impact impact,
      Map<String, Boolean> stockTypeCache,
      Map<Long, Boolean> filialCache) {
    String stockTypeKey = impact.estoqueTipoId()
      + "|" + command.tenantId()
      + "|" + command.catalogConfigurationId()
      + "|" + impact.agrupadorEmpresaId();

    if (!stockTypeCache.containsKey(stockTypeKey)) {
      boolean validStockType = stockTypeRepository
        .findByIdAndTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrue(
          impact.estoqueTipoId(),
          command.tenantId(),
          command.catalogConfigurationId(),
          impact.agrupadorEmpresaId())
        .isPresent();
      stockTypeCache.put(stockTypeKey, validStockType);
    }
    if (!Boolean.TRUE.equals(stockTypeCache.get(stockTypeKey))) {
      throw new IllegalArgumentException("catalog_stock_type_not_found");
    }

    if (!filialCache.containsKey(impact.filialId())) {
      filialCache.put(impact.filialId(), empresaRepository.existsByIdAndTenantId(impact.filialId(), command.tenantId()));
    }
    if (!Boolean.TRUE.equals(filialCache.get(impact.filialId()))) {
      throw new IllegalArgumentException("catalog_stock_filial_not_found");
    }
  }

  private CatalogStockBalance findOrCreateBalanceWithLock(Command command, BalanceKey key) {
    return balanceRepository
      .findWithLockByTenantIdAndCatalogTypeAndCatalogoIdAndAgrupadorEmpresaIdAndEstoqueTipoIdAndFilialId(
        command.tenantId(),
        command.catalogType(),
        command.catalogoId(),
        key.agrupadorEmpresaId(),
        key.estoqueTipoId(),
        key.filialId())
      .orElseGet(() -> createBalance(command, key));
  }

  private CatalogStockBalance createBalance(Command command, BalanceKey key) {
    CatalogStockBalance balance = new CatalogStockBalance();
    balance.setTenantId(command.tenantId());
    balance.setCatalogType(command.catalogType());
    balance.setCatalogoId(command.catalogoId());
    balance.setCatalogConfigurationId(command.catalogConfigurationId());
    balance.setAgrupadorEmpresaId(key.agrupadorEmpresaId());
    balance.setEstoqueTipoId(key.estoqueTipoId());
    balance.setFilialId(key.filialId());
    balance.setQuantidadeAtual(BigDecimal.ZERO.setScale(6, java.math.RoundingMode.HALF_UP));
    balance.setPrecoAtual(BigDecimal.ZERO.setScale(6, java.math.RoundingMode.HALF_UP));

    try {
      return balanceRepository.saveAndFlush(balance);
    } catch (DataIntegrityViolationException ex) {
      return balanceRepository
        .findWithLockByTenantIdAndCatalogTypeAndCatalogoIdAndAgrupadorEmpresaIdAndEstoqueTipoIdAndFilialId(
          command.tenantId(),
          command.catalogType(),
          command.catalogoId(),
          key.agrupadorEmpresaId(),
          key.estoqueTipoId(),
          key.filialId())
        .orElseThrow(() -> ex);
    }
  }

  private BigDecimal valueForMetric(CatalogStockBalance balance, CatalogMovementMetricType metricType) {
    return metricType == CatalogMovementMetricType.QUANTIDADE ? balance.getQuantidadeAtual() : balance.getPrecoAtual();
  }

  private void applyMetric(CatalogStockBalance balance, CatalogMovementMetricType metricType, BigDecimal value) {
    BigDecimal normalized = value.setScale(6, java.math.RoundingMode.HALF_UP);
    if (metricType == CatalogMovementMetricType.QUANTIDADE) {
      balance.setQuantidadeAtual(normalized);
    } else {
      balance.setPrecoAtual(normalized);
    }
  }
}
