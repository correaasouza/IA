package com.ia.app.service;

import com.ia.app.domain.TenantUnit;
import com.ia.app.domain.TenantUnitConversion;
import com.ia.app.dto.TenantUnitConversionRequest;
import com.ia.app.dto.TenantUnitConversionResponse;
import com.ia.app.repository.TenantUnitConversionRepository;
import com.ia.app.repository.TenantUnitRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantUnitConversionService {

  private final TenantUnitConversionRepository conversionRepository;
  private final TenantUnitRepository tenantUnitRepository;

  public TenantUnitConversionService(
      TenantUnitConversionRepository conversionRepository,
      TenantUnitRepository tenantUnitRepository) {
    this.conversionRepository = conversionRepository;
    this.tenantUnitRepository = tenantUnitRepository;
  }

  @Transactional(readOnly = true)
  public List<TenantUnitConversionResponse> list() {
    Long tenantId = requireTenant();
    List<TenantUnitConversion> rows = conversionRepository.findAllByTenantIdOrderByUnidadeOrigemIdAscUnidadeDestinoIdAsc(tenantId);

    Set<UUID> unitIds = rows.stream()
      .flatMap(row -> java.util.stream.Stream.of(row.getUnidadeOrigemId(), row.getUnidadeDestinoId()))
      .filter(id -> id != null)
      .collect(java.util.stream.Collectors.toSet());
    Map<UUID, String> siglaById = loadSiglas(tenantId, unitIds);

    return rows.stream().map(row -> toResponse(row, siglaById)).toList();
  }

  @Transactional(readOnly = true)
  public TenantUnitConversionResponse get(UUID id) {
    Long tenantId = requireTenant();
    TenantUnitConversion conversion = findByTenant(id, tenantId);
    Map<UUID, String> siglaById = loadSiglas(tenantId, Set.of(
      conversion.getUnidadeOrigemId(),
      conversion.getUnidadeDestinoId()));
    return toResponse(conversion, siglaById);
  }

  @Transactional
  public TenantUnitConversionResponse create(TenantUnitConversionRequest request) {
    Long tenantId = requireTenant();
    UUID origemId = requireUnitInTenant(tenantId, request.unidadeOrigemId(), "tenant_unit_conversion_origem_invalid");
    UUID destinoId = requireUnitInTenant(tenantId, request.unidadeDestinoId(), "tenant_unit_conversion_destino_invalid");
    validatePair(origemId, destinoId);
    validateUniqueness(tenantId, origemId, destinoId, null);

    TenantUnitConversion conversion = new TenantUnitConversion();
    conversion.setTenantId(tenantId);
    conversion.setUnidadeOrigemId(origemId);
    conversion.setUnidadeDestinoId(destinoId);
    conversion.setFator(normalizeFactor(request.fator()));

    TenantUnitConversion saved = conversionRepository.save(conversion);
    Map<UUID, String> siglaById = loadSiglas(tenantId, Set.of(origemId, destinoId));
    return toResponse(saved, siglaById);
  }

  @Transactional
  public TenantUnitConversionResponse update(UUID id, TenantUnitConversionRequest request) {
    Long tenantId = requireTenant();
    TenantUnitConversion conversion = findByTenant(id, tenantId);

    UUID origemId = requireUnitInTenant(tenantId, request.unidadeOrigemId(), "tenant_unit_conversion_origem_invalid");
    UUID destinoId = requireUnitInTenant(tenantId, request.unidadeDestinoId(), "tenant_unit_conversion_destino_invalid");
    validatePair(origemId, destinoId);
    validateUniqueness(tenantId, origemId, destinoId, conversion.getId());

    conversion.setUnidadeOrigemId(origemId);
    conversion.setUnidadeDestinoId(destinoId);
    conversion.setFator(normalizeFactor(request.fator()));

    TenantUnitConversion saved = conversionRepository.save(conversion);
    Map<UUID, String> siglaById = loadSiglas(tenantId, Set.of(origemId, destinoId));
    return toResponse(saved, siglaById);
  }

  @Transactional
  public void delete(UUID id) {
    Long tenantId = requireTenant();
    TenantUnitConversion conversion = findByTenant(id, tenantId);
    conversionRepository.delete(conversion);
  }

  private TenantUnitConversionResponse toResponse(TenantUnitConversion row, Map<UUID, String> siglaById) {
    return new TenantUnitConversionResponse(
      row.getId(),
      row.getTenantId(),
      row.getUnidadeOrigemId(),
      siglaById.get(row.getUnidadeOrigemId()),
      row.getUnidadeDestinoId(),
      siglaById.get(row.getUnidadeDestinoId()),
      row.getFator());
  }

  private Map<UUID, String> loadSiglas(Long tenantId, Set<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    Map<UUID, String> siglaById = new HashMap<>();
    for (TenantUnit unit : tenantUnitRepository.findAllByTenantIdAndIdIn(tenantId, ids)) {
      siglaById.put(unit.getId(), unit.getSigla());
    }
    return siglaById;
  }

  private TenantUnitConversion findByTenant(UUID id, Long tenantId) {
    if (id == null) {
      throw new IllegalArgumentException("tenant_unit_conversion_id_invalid");
    }
    return conversionRepository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("tenant_unit_conversion_not_found"));
  }

  private UUID requireUnitInTenant(Long tenantId, UUID id, String errorCode) {
    if (id == null || !tenantUnitRepository.existsByTenantIdAndId(tenantId, id)) {
      throw new IllegalArgumentException(errorCode);
    }
    return id;
  }

  private void validatePair(UUID origemId, UUID destinoId) {
    if (origemId.equals(destinoId)) {
      throw new IllegalArgumentException("tenant_unit_conversion_pair_invalid");
    }
  }

  private void validateUniqueness(Long tenantId, UUID origemId, UUID destinoId, UUID currentId) {
    boolean exists = currentId == null
      ? conversionRepository.existsByTenantIdAndUnidadeOrigemIdAndUnidadeDestinoId(tenantId, origemId, destinoId)
      : conversionRepository.existsByTenantIdAndUnidadeOrigemIdAndUnidadeDestinoIdAndIdNot(tenantId, origemId, destinoId, currentId);
    if (exists) {
      throw new IllegalArgumentException("tenant_unit_conversion_duplicada");
    }
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private BigDecimal normalizeFactor(BigDecimal value) {
    if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("tenant_unit_conversion_factor_invalid");
    }
    return value.setScale(UnitConversionService.FACTOR_SCALE, RoundingMode.HALF_UP);
  }
}
