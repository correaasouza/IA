package com.ia.app.service;

import com.ia.app.domain.OfficialUnit;
import com.ia.app.domain.TenantUnit;
import com.ia.app.dto.TenantUnitRequest;
import com.ia.app.dto.TenantUnitResponse;
import com.ia.app.repository.OfficialUnitRepository;
import com.ia.app.repository.TenantUnitRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantUnitService {

  public record ReconcileResult(Long tenantId, int createdMirrors) {}

  private final TenantUnitRepository tenantUnitRepository;
  private final OfficialUnitRepository officialUnitRepository;
  private final TenantUnitMirrorService tenantUnitMirrorService;

  public TenantUnitService(
      TenantUnitRepository tenantUnitRepository,
      OfficialUnitRepository officialUnitRepository,
      TenantUnitMirrorService tenantUnitMirrorService) {
    this.tenantUnitRepository = tenantUnitRepository;
    this.officialUnitRepository = officialUnitRepository;
    this.tenantUnitMirrorService = tenantUnitMirrorService;
  }

  @Transactional(readOnly = true)
  public List<TenantUnitResponse> list(String text) {
    Long tenantId = requireTenant();
    String normalizedText = normalizeOptional(text);

    List<TenantUnit> units = tenantUnitRepository.findAllByTenantIdOrderBySiglaAsc(tenantId).stream()
      .filter(unit -> {
        if (normalizedText == null) {
          return true;
        }
        String sigla = unit.getSigla() == null ? "" : unit.getSigla().toLowerCase(Locale.ROOT);
        String nome = unit.getNome() == null ? "" : unit.getNome().toLowerCase(Locale.ROOT);
        return sigla.contains(normalizedText) || nome.contains(normalizedText);
      })
      .sorted(Comparator.comparing(TenantUnit::getSigla, String.CASE_INSENSITIVE_ORDER))
      .toList();

    Map<UUID, OfficialUnit> officialById = loadOfficialMap(units.stream().map(TenantUnit::getUnidadeOficialId).toList());
    return units.stream().map(unit -> toResponse(unit, officialById.get(unit.getUnidadeOficialId()))).toList();
  }

  @Transactional(readOnly = true)
  public TenantUnitResponse get(UUID id) {
    Long tenantId = requireTenant();
    TenantUnit unit = findByTenant(id, tenantId);
    OfficialUnit official = findOfficial(unit.getUnidadeOficialId());
    return toResponse(unit, official);
  }

  @Transactional
  public TenantUnitResponse create(TenantUnitRequest request) {
    Long tenantId = requireTenant();
    OfficialUnit official = findOfficial(request.unidadeOficialId());
    String sigla = normalizeSigla(request.sigla());
    validateSiglaUniqueness(tenantId, sigla, null);

    TenantUnit unit = new TenantUnit();
    unit.setTenantId(tenantId);
    unit.setUnidadeOficialId(official.getId());
    unit.setSigla(sigla);
    unit.setNome(normalizeNome(request.nome()));
    unit.setFatorParaOficial(normalizeFactor(request.fatorParaOficial()));
    unit.setSystemMirror(false);

    TenantUnit saved = tenantUnitRepository.save(unit);
    return toResponse(saved, official);
  }

  @Transactional
  public TenantUnitResponse update(UUID id, TenantUnitRequest request) {
    Long tenantId = requireTenant();
    TenantUnit unit = findByTenant(id, tenantId);
    OfficialUnit official = findOfficial(request.unidadeOficialId());
    String sigla = normalizeSigla(request.sigla());
    validateSiglaUniqueness(tenantId, sigla, unit.getId());

    unit.setUnidadeOficialId(official.getId());
    unit.setSigla(sigla);
    unit.setNome(normalizeNome(request.nome()));
    unit.setFatorParaOficial(normalizeFactor(request.fatorParaOficial()));

    TenantUnit saved = tenantUnitRepository.save(unit);
    return toResponse(saved, official);
  }

  @Transactional
  public void delete(UUID id) {
    Long tenantId = requireTenant();
    TenantUnit unit = findByTenant(id, tenantId);
    if (unit.isSystemMirror()) {
      throw new IllegalArgumentException("tenant_unit_mirror_delete_not_allowed");
    }
    try {
      tenantUnitRepository.delete(unit);
      tenantUnitRepository.flush();
    } catch (DataIntegrityViolationException ex) {
      throw new IllegalArgumentException("tenant_unit_in_use");
    }
  }

  @Transactional
  public ReconcileResult reconcileCurrentTenant() {
    Long tenantId = requireTenant();
    int created = tenantUnitMirrorService.seedMissingMirrorsForTenant(tenantId);
    return new ReconcileResult(tenantId, created);
  }

  @Transactional
  public int seedMissingMirrorsForTenant(Long tenantId) {
    return tenantUnitMirrorService.seedMissingMirrorsForTenant(tenantId);
  }

  private Map<UUID, OfficialUnit> loadOfficialMap(List<UUID> ids) {
    Map<UUID, OfficialUnit> map = new HashMap<>();
    for (UUID id : ids) {
      if (id == null || map.containsKey(id)) {
        continue;
      }
      officialUnitRepository.findById(id).ifPresent(unit -> map.put(id, unit));
    }
    return map;
  }

  private TenantUnitResponse toResponse(TenantUnit unit, OfficialUnit official) {
    return new TenantUnitResponse(
      unit.getId(),
      unit.getTenantId(),
      unit.getUnidadeOficialId(),
      official == null ? null : official.getCodigoOficial(),
      official == null ? null : official.getDescricao(),
      official != null && official.isAtivo(),
      unit.getSigla(),
      unit.getNome(),
      unit.getFatorParaOficial(),
      unit.isSystemMirror());
  }

  private void validateSiglaUniqueness(Long tenantId, String sigla, UUID currentId) {
    boolean exists = currentId == null
      ? tenantUnitRepository.existsByTenantIdAndSiglaIgnoreCase(tenantId, sigla)
      : tenantUnitRepository.existsByTenantIdAndSiglaIgnoreCaseAndIdNot(tenantId, sigla, currentId);
    if (exists) {
      throw new IllegalArgumentException("tenant_unit_sigla_duplicada");
    }
  }

  private TenantUnit findByTenant(UUID id, Long tenantId) {
    if (id == null) {
      throw new IllegalArgumentException("tenant_unit_id_invalid");
    }
    return tenantUnitRepository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("tenant_unit_not_found"));
  }

  private OfficialUnit findOfficial(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("tenant_unit_official_required");
    }
    return officialUnitRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("official_unit_not_found"));
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private String normalizeSigla(String value) {
    String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("tenant_unit_sigla_required");
    }
    if (normalized.length() > 20) {
      normalized = normalized.substring(0, 20);
    }
    return normalized;
  }

  private String normalizeNome(String value) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("tenant_unit_nome_required");
    }
    if (normalized.length() > 160) {
      normalized = normalized.substring(0, 160);
    }
    return normalized;
  }

  private BigDecimal normalizeFactor(BigDecimal value) {
    if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("tenant_unit_factor_invalid");
    }
    return value.setScale(UnitConversionService.FACTOR_SCALE, RoundingMode.HALF_UP);
  }

  private String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }
}
