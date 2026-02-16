package com.ia.app.web;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogMovementMetricType;
import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.dto.CatalogMovementResponse;
import com.ia.app.dto.CatalogStockBalanceViewResponse;
import com.ia.app.service.CatalogStockQueryService;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
      @RequestParam(required = false) String metricType,
      @RequestParam(required = false) Long estoqueTipoId,
      @RequestParam(required = false) Long filialId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
      Pageable pageable) {
    CatalogConfigurationType parsedType = CatalogConfigurationType.from(type);
    CatalogMovementOriginType parsedOrigin = CatalogMovementOriginType.fromNullable(origemTipo);
    CatalogMovementMetricType parsedMetric = CatalogMovementMetricType.fromNullable(metricType);

    return ResponseEntity.ok(queryService.loadLedger(
      parsedType,
      catalogoId,
      agrupadorId,
      parsedOrigin,
      fromDate,
      toDate,
      parsedMetric,
      estoqueTipoId,
      filialId,
      pageable));
  }
}
