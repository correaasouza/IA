package com.ia.app.service;

import com.ia.app.domain.CatalogItemCodeSeq;
import com.ia.app.repository.CatalogConfigurationByGroupRepository;
import com.ia.app.repository.CatalogItemCodeSeqRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogItemCodeService {

  private final CatalogItemCodeSeqRepository repository;
  private final CatalogConfigurationByGroupRepository configByGroupRepository;

  public CatalogItemCodeService(
      CatalogItemCodeSeqRepository repository,
      CatalogConfigurationByGroupRepository configByGroupRepository) {
    this.repository = repository;
    this.configByGroupRepository = configByGroupRepository;
  }

  @Transactional
  public Long proximoCodigo(Long tenantId, Long catalogConfigurationId, Long agrupadorEmpresaId) {
    if (tenantId == null || catalogConfigurationId == null || agrupadorEmpresaId == null) {
      throw new IllegalArgumentException("catalog_item_codigo_scope_invalid");
    }

    configByGroupRepository
      .findWithLockByTenantIdAndCatalogConfigurationIdAndAgrupadorIdAndActiveTrue(
        tenantId, catalogConfigurationId, agrupadorEmpresaId)
      .orElseThrow(() -> new IllegalArgumentException("catalog_context_sem_grupo"));

    CatalogItemCodeSeq seq = repository
      .findWithLockByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaId(
        tenantId, catalogConfigurationId, agrupadorEmpresaId)
      .orElseGet(() -> criarInicial(tenantId, catalogConfigurationId, agrupadorEmpresaId));

    if (seq.getNextValue() == null || seq.getNextValue() < 1) {
      seq.setNextValue(1L);
    }
    Long codigo = seq.getNextValue();
    seq.setNextValue(codigo + 1);
    repository.save(seq);
    return codigo;
  }

  private CatalogItemCodeSeq criarInicial(Long tenantId, Long catalogConfigurationId, Long agrupadorEmpresaId) {
    CatalogItemCodeSeq seq = new CatalogItemCodeSeq();
    seq.setTenantId(tenantId);
    seq.setCatalogConfigurationId(catalogConfigurationId);
    seq.setAgrupadorEmpresaId(agrupadorEmpresaId);
    seq.setNextValue(1L);
    try {
      return repository.save(seq);
    } catch (DataIntegrityViolationException ex) {
      return repository
        .findWithLockByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaId(
          tenantId, catalogConfigurationId, agrupadorEmpresaId)
        .orElseThrow(() -> ex);
    }
  }
}
