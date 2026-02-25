package com.ia.app.web;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogMovementMetricType;
import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.dto.CatalogMovementResponse;
import com.ia.app.dto.CatalogStockBalanceViewResponse;
import com.ia.app.service.CatalogStockQueryService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/{type}")
public class CatalogStockController {

  private final CatalogStockQueryService queryService;

  public CatalogStockController(CatalogStockQueryService queryService) {
    this.queryService = queryService;
  }

  @GetMapping("/items/{catalogoId}/stock/balances")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<CatalogStockBalanceViewResponse> balances(
      @PathVariable String type,
      @PathVariable Long catalogoId,
      @RequestParam(required = false) Long agrupadorId,
      @RequestParam(required = false) Long estoqueTipoId,
      @RequestParam(required = false) Long filialId) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    return ResponseEntity.ok(queryService.loadBalanceView(parsedType, catalogoId, agrupadorId, estoqueTipoId, filialId));
  }

  @GetMapping("/items/{catalogoId}/stock/ledger")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<Page<CatalogMovementResponse>> ledger(
      @PathVariable String type,
      @PathVariable Long catalogoId,
      @RequestParam(required = false) Long agrupadorId,
      @RequestParam(required = false) String origemTipo,
      @RequestParam(required = false) String origemCodigo,
      @RequestParam(required = false) Long origemId,
      @RequestParam(required = false) String movimentoTipo,
      @RequestParam(required = false) String usuario,
      @RequestParam(required = false) String metricType,
      @RequestParam(required = false) Long estoqueTipoId,
      @RequestParam(required = false) Long filialId,
      @RequestParam(required = false) String fromDate,
      @RequestParam(required = false) String toDate,
      @RequestParam(required = false) Integer tzOffsetMinutes,
      Pageable pageable) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    CatalogMovementOriginType parsedOrigin = CatalogMovementOriginType.fromNullable(origemTipo);
    CatalogMovementMetricType parsedMetric = CatalogMovementMetricType.fromNullable(metricType);
    Instant parsedFrom = parseLedgerFromDate(fromDate, tzOffsetMinutes);
    Instant parsedTo = parseLedgerToDate(toDate, tzOffsetMinutes);

    return ResponseEntity.ok(queryService.loadLedger(
      parsedType,
      catalogoId,
      agrupadorId,
      parsedOrigin,
      origemCodigo,
      origemId,
      movimentoTipo,
      usuario,
      parsedFrom,
      parsedTo,
      parsedMetric,
      estoqueTipoId,
      filialId,
      pageable));
  }

  private Instant parseLedgerFromDate(String raw, Integer tzOffsetMinutes) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String normalized = raw.trim();
    if (normalized.length() == 10) {
      LocalDate date = LocalDate.parse(normalized);
      return date.atStartOfDay(resolveZoneOffset(tzOffsetMinutes)).toInstant();
    }
    return Instant.parse(normalized);
  }

  private Instant parseLedgerToDate(String raw, Integer tzOffsetMinutes) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String normalized = raw.trim();
    if (normalized.length() == 10) {
      LocalDate date = LocalDate.parse(normalized);
      return date.plusDays(1L)
        .atStartOfDay(resolveZoneOffset(tzOffsetMinutes))
        .toInstant()
        .minusNanos(1L);
    }
    return Instant.parse(normalized);
  }

  private ZoneOffset resolveZoneOffset(Integer tzOffsetMinutes) {
    int minutes = tzOffsetMinutes == null ? 0 : tzOffsetMinutes;
    if (minutes < -1080 || minutes > 1080) {
      throw new IllegalArgumentException("catalog_stock_timezone_invalid");
    }
    return ZoneOffset.ofTotalSeconds(-minutes * 60);
  }
}
