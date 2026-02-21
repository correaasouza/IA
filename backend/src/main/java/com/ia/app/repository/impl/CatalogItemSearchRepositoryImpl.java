package com.ia.app.repository.impl;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.repository.CatalogItemSearchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogItemSearchRepositoryImpl implements CatalogItemSearchRepository {

  private static final Logger log = LoggerFactory.getLogger(CatalogItemSearchRepositoryImpl.class);

  @PersistenceContext
  private EntityManager entityManager;

  private volatile Boolean similaritySupported;

  @Override
  public Page<SearchRow> search(CatalogConfigurationType type, SearchCriteria criteria) {
    String table = resolveTable(type);
    boolean useSimilarity = isSimilaritySupported();
    String sql = buildSearchSql(table, useSimilarity);
    String countSql = buildCountSql(table);

    boolean hasQuery = criteria.q() != null && !criteria.q().isBlank();
    String qNormalized = hasQuery ? criteria.q().trim().toLowerCase() : "";
    String qLike = hasQuery ? criteria.qLike() : "%";

    Query query = entityManager.createNativeQuery(sql);
    applyCommonParams(query, criteria, hasQuery, qNormalized, qLike, useSimilarity);
    query.setFirstResult(Math.max(0, criteria.page()) * Math.max(1, criteria.size()));
    query.setMaxResults(Math.max(1, criteria.size()));

    Query countQuery = entityManager.createNativeQuery(countSql);
    applyCommonParams(countQuery, criteria, hasQuery, qNormalized, qLike, false);

    @SuppressWarnings("unchecked")
    List<Object[]> rows = query.getResultList();
    List<SearchRow> content = new ArrayList<>(rows.size());
    for (Object[] row : rows) {
      content.add(new SearchRow(
        toLong(row[0]),
        toLong(row[1]),
        toStringValue(row[2]),
        toStringValue(row[3]),
        toLong(row[4]),
        toStringValue(row[5]),
        toBoolean(row[6])));
    }

    Number totalNumber = (Number) countQuery.getSingleResult();
    long total = totalNumber == null ? 0L : totalNumber.longValue();
    return new PageImpl<>(content, PageRequest.of(Math.max(0, criteria.page()), Math.max(1, criteria.size())), total);
  }

  private void applyCommonParams(
      Query query,
      SearchCriteria criteria,
      boolean hasQuery,
      String qNormalized,
      String qLike,
      boolean includeQParam) {
    query.setParameter("tenantId", criteria.tenantId());
    query.setParameter("catalogConfigurationId", criteria.catalogConfigurationId());
    query.setParameter("agrupadorEmpresaId", criteria.agrupadorEmpresaId());
    query.setParameter("hasQuery", hasQuery);
    if (includeQParam) {
      query.setParameter("q", qNormalized);
    }
    query.setParameter("qLike", qLike);
    query.setParameter("groupId", criteria.groupId());
    query.setParameter("includeDescendants", criteria.includeDescendants());
    query.setParameter("groupPath", criteria.groupPath());
    query.setParameter("groupPathLike", criteria.groupPathLike());
    query.setParameter("ativo", criteria.ativo());
  }

  private String buildSearchSql(String table, boolean useSimilarity) {
    String orderBy = useSimilarity
      ? """
        order by
          case
            when :hasQuery = true then greatest(
              similarity(lower(i.nome), :q),
              similarity(lower(coalesce(i.descricao, '')), :q),
              similarity(cast(i.codigo as text), :q)
            )
            else 0
          end desc,
          i.codigo asc,
          i.id asc
        """
      : """
        order by i.codigo asc, i.id asc
        """;

    return ("""
      select
        i.id,
        i.codigo,
        i.nome,
        i.descricao,
        i.catalog_group_id,
        g.path,
        i.ativo
      from %s i
      left join catalog_group g
        on g.id = i.catalog_group_id
       and g.tenant_id = i.tenant_id
       and g.catalog_configuration_id = i.catalog_configuration_id
      where i.tenant_id = :tenantId
        and i.catalog_configuration_id = :catalogConfigurationId
        and i.agrupador_empresa_id = :agrupadorEmpresaId
        and (cast(:ativo as boolean) is null or i.ativo = cast(:ativo as boolean))
        and (
          :hasQuery = false
          or lower(i.nome) like :qLike
          or lower(coalesce(i.descricao, '')) like :qLike
          or cast(i.codigo as text) like :qLike
        )
        and (
          cast(:groupId as bigint) is null
          or (:includeDescendants = false and i.catalog_group_id = cast(:groupId as bigint))
          or (
            :includeDescendants = true
            and g.path is not null
            and (
              g.path = cast(:groupPath as varchar)
              or g.path like cast(:groupPathLike as varchar)
            )
          )
        )
      %s
      """).formatted(table, orderBy);
  }

  private String buildCountSql(String table) {
    return """
      select count(1)
      from %s i
      left join catalog_group g
        on g.id = i.catalog_group_id
       and g.tenant_id = i.tenant_id
       and g.catalog_configuration_id = i.catalog_configuration_id
      where i.tenant_id = :tenantId
        and i.catalog_configuration_id = :catalogConfigurationId
        and i.agrupador_empresa_id = :agrupadorEmpresaId
        and (cast(:ativo as boolean) is null or i.ativo = cast(:ativo as boolean))
        and (
          :hasQuery = false
          or lower(i.nome) like :qLike
          or lower(coalesce(i.descricao, '')) like :qLike
          or cast(i.codigo as text) like :qLike
        )
        and (
          cast(:groupId as bigint) is null
          or (:includeDescendants = false and i.catalog_group_id = cast(:groupId as bigint))
          or (
            :includeDescendants = true
            and g.path is not null
            and (
              g.path = cast(:groupPath as varchar)
              or g.path like cast(:groupPathLike as varchar)
            )
          )
        )
      """.formatted(table);
  }

  private String resolveTable(CatalogConfigurationType type) {
    return switch (type) {
      case PRODUCTS -> "catalog_product";
      case SERVICES -> "catalog_service_item";
    };
  }

  private boolean isSimilaritySupported() {
    Boolean cached = similaritySupported;
    if (cached != null) {
      return cached;
    }
    synchronized (this) {
      if (similaritySupported != null) {
        return similaritySupported;
      }
      try {
        Object result = entityManager
          .createNativeQuery("select to_regproc('similarity') is not null")
          .getSingleResult();
        similaritySupported = Boolean.TRUE.equals(result);
      } catch (Exception ex) {
        log.warn("Fallback de busca sem similarity(): {}", ex.getMessage());
        similaritySupported = false;
      }
      return similaritySupported;
    }
  }

  private Long toLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(String.valueOf(value));
  }

  private Boolean toBoolean(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean b) {
      return b;
    }
    if (value instanceof Number number) {
      return number.intValue() != 0;
    }
    return Boolean.parseBoolean(String.valueOf(value));
  }

  private String toStringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
