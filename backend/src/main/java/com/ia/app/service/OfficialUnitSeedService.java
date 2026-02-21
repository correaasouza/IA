package com.ia.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.domain.OfficialUnit;
import com.ia.app.domain.OfficialUnitOrigin;
import com.ia.app.repository.OfficialUnitRepository;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficialUnitSeedService {

  private static final Logger log = LoggerFactory.getLogger(OfficialUnitSeedService.class);
  private static final String DEFAULT_RESOURCE_PATH = "classpath:seeds/nfe-official-units.json";

  private final OfficialUnitRepository officialUnitRepository;
  private final TenantUnitMirrorService tenantUnitMirrorService;
  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;

  @Value("${units.seed.official-on-startup:true}")
  private boolean seedOfficialOnStartup;

  @Value("${units.seed.official-resource:classpath:seeds/nfe-official-units.json}")
  private String seedResourcePath;

  @Value("${units.seed.reconcile-tenants-on-startup:true}")
  private boolean reconcileTenantsOnStartup;

  public OfficialUnitSeedService(
      OfficialUnitRepository officialUnitRepository,
      TenantUnitMirrorService tenantUnitMirrorService,
      ObjectMapper objectMapper,
      ResourceLoader resourceLoader) {
    this.officialUnitRepository = officialUnitRepository;
    this.tenantUnitMirrorService = tenantUnitMirrorService;
    this.objectMapper = objectMapper;
    this.resourceLoader = resourceLoader;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    if (seedOfficialOnStartup) {
      seedOfficialUnitsFromResource();
    }
    if (reconcileTenantsOnStartup) {
      tenantUnitMirrorService.reconcileAllTenants();
    }
  }

  @Transactional
  public SeedResult seedOfficialUnitsFromResource() {
    Resource resource = resourceLoader.getResource(seedResourcePath == null || seedResourcePath.isBlank()
      ? DEFAULT_RESOURCE_PATH
      : seedResourcePath);

    if (!resource.exists()) {
      log.warn("official_unit seed skipped: resource not found path={}", seedResourcePath);
      return new SeedResult(0, 0, 0);
    }

    List<SeedRow> rows;
    try (InputStream stream = resource.getInputStream()) {
      rows = objectMapper.readValue(stream, new TypeReference<List<SeedRow>>() {});
    } catch (Exception ex) {
      throw new IllegalStateException("official_unit_seed_resource_invalid", ex);
    }

    int inserted = 0;
    int updated = 0;
    int unchanged = 0;
    for (SeedRow row : rows) {
      String code = normalizeCode(row.codigoOficial());
      String description = normalizeDescription(row.descricao(), code);

      OfficialUnit existing = officialUnitRepository.findByCodigoOficialIgnoreCase(code).orElse(null);
      if (existing == null) {
        OfficialUnit unit = new OfficialUnit();
        unit.setCodigoOficial(code);
        unit.setDescricao(description);
        unit.setAtivo(true);
        unit.setOrigem(OfficialUnitOrigin.NFE_TABELA_UNIDADE_COMERCIAL);
        officialUnitRepository.save(unit);
        inserted += 1;
        continue;
      }

      boolean dirty = false;
      if (existing.getOrigem() == OfficialUnitOrigin.NFE_TABELA_UNIDADE_COMERCIAL
        && !description.equals(existing.getDescricao())) {
        existing.setDescricao(description);
        dirty = true;
      }
      if (existing.getOrigem() == null) {
        existing.setOrigem(OfficialUnitOrigin.NFE_TABELA_UNIDADE_COMERCIAL);
        dirty = true;
      }

      if (dirty) {
        officialUnitRepository.save(existing);
        updated += 1;
      } else {
        unchanged += 1;
      }
    }

    log.info("official_unit seed executed: inserted={} updated={} unchanged={} source={}",
      inserted,
      updated,
      unchanged,
      resource.getFilename());

    return new SeedResult(inserted, updated, unchanged);
  }

  private String normalizeCode(String value) {
    String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new IllegalStateException("official_unit_seed_row_invalid");
    }
    if (normalized.length() > 20) {
      normalized = normalized.substring(0, 20);
    }
    return normalized;
  }

  private String normalizeDescription(String value, String fallbackCode) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.isBlank()) {
      normalized = fallbackCode;
    }
    if (normalized.length() > 160) {
      normalized = normalized.substring(0, 160);
    }
    return normalized;
  }

  public record SeedResult(int inserted, int updated, int unchanged) {}

  private record SeedRow(String codigoOficial, String descricao) {}
}
