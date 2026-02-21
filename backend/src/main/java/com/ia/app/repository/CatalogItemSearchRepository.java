package com.ia.app.repository;

import com.ia.app.domain.CatalogConfigurationType;
import org.springframework.data.domain.Page;

public interface CatalogItemSearchRepository {

  record SearchCriteria(
    Long tenantId,
    Long catalogConfigurationId,
    Long agrupadorEmpresaId,
    String q,
    String qLike,
    Long groupId,
    boolean includeDescendants,
    String groupPath,
    String groupPathLike,
    Boolean ativo,
    int page,
    int size
  ) {}

  record SearchRow(
    Long id,
    Long codigo,
    String nome,
    String descricao,
    Long groupId,
    String groupPath,
    Boolean ativo
  ) {}

  Page<SearchRow> search(CatalogConfigurationType type, SearchCriteria criteria);
}