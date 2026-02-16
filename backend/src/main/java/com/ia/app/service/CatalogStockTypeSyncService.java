package com.ia.app.service;

import com.ia.app.domain.CatalogStockType;
import com.ia.app.repository.CatalogStockTypeRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogStockTypeSyncService {

  private static final String DEFAULT_CODE = "GERAL";
  private static final String DEFAULT_NAME = "Estoque Geral";

  private final CatalogStockTypeRepository repository;

  public CatalogStockTypeSyncService(CatalogStockTypeRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public CatalogStockType ensureDefaultForGroup(Long tenantId, Long catalogConfigurationId, Long agrupadorEmpresaId) {
    return ensureByCode(
      tenantId,
      catalogConfigurationId,
      agrupadorEmpresaId,
      DEFAULT_CODE,
      DEFAULT_NAME,
      1);
  }

  @Transactional
  public CatalogStockType ensureByCode(
      Long tenantId,
      Long catalogConfigurationId,
      Long agrupadorEmpresaId,
      String code,
      String name,
      Integer ordem) {
    String normalizedCode = normalizeCode(code);
    String normalizedName = normalizeName(name, normalizedCode);
    int normalizedOrder = (ordem == null || ordem <= 0) ? 1 : ordem;

    return repository.findByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndCodigoAndActiveTrue(
        tenantId, catalogConfigurationId, agrupadorEmpresaId, normalizedCode)
      .orElseGet(() -> createStockType(
        tenantId,
        catalogConfigurationId,
        agrupadorEmpresaId,
        normalizedCode,
        normalizedName,
        normalizedOrder));
  }

  @Transactional
  public void onGroupRemoved(Long tenantId, Long catalogConfigurationId, Long agrupadorEmpresaId) {
    repository.findAllByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrueOrderByOrdemAscNomeAsc(
        tenantId,
        catalogConfigurationId,
        agrupadorEmpresaId)
      .forEach(stockType -> stockType.setActive(false));
  }

  private CatalogStockType createStockType(
      Long tenantId,
      Long catalogConfigurationId,
      Long agrupadorEmpresaId,
      String code,
      String name,
      Integer order) {
    CatalogStockType entity = new CatalogStockType();
    entity.setTenantId(tenantId);
    entity.setCatalogConfigurationId(catalogConfigurationId);
    entity.setAgrupadorEmpresaId(agrupadorEmpresaId);
    entity.setCodigo(code);
    entity.setNome(name);
    entity.setOrdem(order);
    entity.setActive(true);
    try {
      return repository.saveAndFlush(entity);
    } catch (DataIntegrityViolationException ex) {
      return repository.findByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndCodigoAndActiveTrue(
          tenantId,
          catalogConfigurationId,
          agrupadorEmpresaId,
          code)
        .orElseThrow(() -> ex);
    }
  }

  private String normalizeCode(String value) {
    String raw = value == null ? "" : value.trim();
    if (raw.isEmpty()) {
      raw = DEFAULT_CODE;
    }
    raw = raw
      .replace(' ', '_')
      .replace('-', '_')
      .toUpperCase(Locale.ROOT);
    return raw.length() > 40 ? raw.substring(0, 40) : raw;
  }

  private String normalizeName(String value, String normalizedCode) {
    String raw = value == null ? "" : value.trim();
    if (raw.isEmpty()) {
      raw = "Estoque " + normalizedCode;
    }
    return raw.length() > 120 ? raw.substring(0, 120) : raw;
  }
}
