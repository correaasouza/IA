package com.ia.app.service;

import com.ia.app.domain.Locatario;
import com.ia.app.domain.OfficialUnit;
import com.ia.app.domain.TenantUnit;
import com.ia.app.repository.LocatarioRepository;
import com.ia.app.repository.OfficialUnitRepository;
import com.ia.app.repository.TenantUnitRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantUnitMirrorService {

  private static final Logger log = LoggerFactory.getLogger(TenantUnitMirrorService.class);

  private final OfficialUnitRepository officialUnitRepository;
  private final TenantUnitRepository tenantUnitRepository;
  private final LocatarioRepository locatarioRepository;

  public TenantUnitMirrorService(
      OfficialUnitRepository officialUnitRepository,
      TenantUnitRepository tenantUnitRepository,
      LocatarioRepository locatarioRepository) {
    this.officialUnitRepository = officialUnitRepository;
    this.tenantUnitRepository = tenantUnitRepository;
    this.locatarioRepository = locatarioRepository;
  }

  @Transactional
  public int seedMissingMirrorsForTenant(Long tenantId) {
    if (tenantId == null || tenantId <= 0) {
      return 0;
    }

    List<OfficialUnit> officialUnits = officialUnitRepository.findAllByOrderByCodigoOficialAsc();
    int created = 0;
    for (OfficialUnit officialUnit : officialUnits) {
      if (officialUnit == null || officialUnit.getId() == null) {
        continue;
      }

      if (tenantUnitRepository.findByTenantIdAndUnidadeOficialIdAndSystemMirrorTrue(tenantId, officialUnit.getId()).isPresent()) {
        continue;
      }

      TenantUnit mirror = new TenantUnit();
      mirror.setTenantId(tenantId);
      mirror.setUnidadeOficialId(officialUnit.getId());
      mirror.setSigla(resolveMirrorSigla(tenantId, officialUnit.getCodigoOficial(), officialUnit.getId()));
      mirror.setNome(normalizeName(officialUnit.getDescricao(), mirror.getSigla()));
      mirror.setFatorParaOficial(BigDecimal.ONE.setScale(UnitConversionService.FACTOR_SCALE, java.math.RoundingMode.HALF_UP));
      mirror.setSystemMirror(true);
      mirror.setPadrao(false);
      tenantUnitRepository.save(mirror);
      created += 1;
    }

    ensureDefaultUnitForTenant(tenantId);

    if (created > 0) {
      log.info("tenant_unit mirror seed: tenant={} created={} officialUnits={}", tenantId, created, officialUnits.size());
    }
    return created;
  }

  @Transactional
  public void reconcileAllTenants() {
    for (Locatario tenant : locatarioRepository.findAll()) {
      if (tenant == null || tenant.getId() == null) {
        continue;
      }
      seedMissingMirrorsForTenant(tenant.getId());
    }
  }

  private String resolveMirrorSigla(Long tenantId, String baseCode, UUID officialUnitId) {
    String normalizedBase = normalizeCode(baseCode);
    if (!tenantUnitRepository.existsByTenantIdAndSiglaIgnoreCase(tenantId, normalizedBase)) {
      return normalizedBase;
    }

    String fallbackBase = normalizedBase;
    if (fallbackBase.length() > 17) {
      fallbackBase = fallbackBase.substring(0, 17);
    }
    String suffix = "OF";
    String candidate = fallbackBase + suffix;
    if (!tenantUnitRepository.existsByTenantIdAndSiglaIgnoreCase(tenantId, candidate)) {
      return candidate;
    }

    String token = officialUnitId.toString().replace("-", "").substring(0, 4).toUpperCase(Locale.ROOT);
    String composed = (fallbackBase + token);
    if (composed.length() > 20) {
      composed = composed.substring(0, 20);
    }
    if (!tenantUnitRepository.existsByTenantIdAndSiglaIgnoreCase(tenantId, composed)) {
      return composed;
    }

    for (int i = 2; i < 100; i++) {
      String next = fallbackBase + i;
      if (next.length() > 20) {
        next = next.substring(0, 20);
      }
      if (!tenantUnitRepository.existsByTenantIdAndSiglaIgnoreCase(tenantId, next)) {
        return next;
      }
    }
    throw new IllegalStateException("tenant_unit_mirror_sigla_exhausted");
  }

  private String normalizeCode(String value) {
    String normalized = value == null ? "UN" : value.trim().toUpperCase(Locale.ROOT);
    if (normalized.isBlank()) {
      normalized = "UN";
    }
    if (normalized.length() > 20) {
      normalized = normalized.substring(0, 20);
    }
    return normalized;
  }

  private String normalizeName(String value, String fallback) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.isBlank()) {
      normalized = fallback;
    }
    if (normalized.length() > 160) {
      normalized = normalized.substring(0, 160);
    }
    return normalized;
  }

  private void ensureDefaultUnitForTenant(Long tenantId) {
    if (tenantUnitRepository.existsByTenantIdAndPadraoTrue(tenantId)) {
      return;
    }

    TenantUnit selected = tenantUnitRepository.findByTenantIdAndSiglaIgnoreCase(tenantId, "UN").orElse(null);
    if (selected == null) {
      selected = tenantUnitRepository.findAllByTenantIdOrderBySiglaAsc(tenantId).stream().findFirst().orElse(null);
    }
    if (selected == null) {
      return;
    }

    selected.setPadrao(true);
    tenantUnitRepository.save(selected);
  }
}
