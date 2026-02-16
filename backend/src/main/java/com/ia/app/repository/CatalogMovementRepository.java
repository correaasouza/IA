package com.ia.app.repository;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogMovement;
import com.ia.app.domain.CatalogMovementMetricType;
import com.ia.app.domain.CatalogMovementOriginType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogMovementRepository extends JpaRepository<CatalogMovement, Long> {

  Optional<CatalogMovement> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);

  @Query(
    value = """
    select m
    from CatalogMovement m
    where m.tenantId = :tenantId
      and m.catalogType = :catalogType
      and m.catalogoId = :catalogoId
      and (
        :agrupadorEmpresaId is null
        or exists (
          select 1
          from CatalogMovementLine lineGroup
          where lineGroup.tenantId = :tenantId
            and lineGroup.movementId = m.id
            and lineGroup.agrupadorEmpresaId = :agrupadorEmpresaId
        )
      )
      and (:origemTipo is null or m.origemMovimentacaoTipo = :origemTipo)
      and m.dataHoraMovimentacao >= :fromDate
      and m.dataHoraMovimentacao <= :toDate
      and (
        (:metricType is null and :estoqueTipoId is null and :filialId is null)
        or exists (
          select 1
          from CatalogMovementLine l
          where l.tenantId = :tenantId
            and l.movementId = m.id
            and (:metricType is null or l.metricType = :metricType)
            and (:estoqueTipoId is null or l.estoqueTipoId = :estoqueTipoId)
            and (:filialId is null or l.filialId = :filialId)
        )
      )
    order by m.dataHoraMovimentacao desc, m.id desc
    """,
    countQuery = """
    select count(m)
    from CatalogMovement m
    where m.tenantId = :tenantId
      and m.catalogType = :catalogType
      and m.catalogoId = :catalogoId
      and (
        :agrupadorEmpresaId is null
        or exists (
          select 1
          from CatalogMovementLine lineGroup
          where lineGroup.tenantId = :tenantId
            and lineGroup.movementId = m.id
            and lineGroup.agrupadorEmpresaId = :agrupadorEmpresaId
        )
      )
      and (:origemTipo is null or m.origemMovimentacaoTipo = :origemTipo)
      and m.dataHoraMovimentacao >= :fromDate
      and m.dataHoraMovimentacao <= :toDate
      and (
        (:metricType is null and :estoqueTipoId is null and :filialId is null)
        or exists (
          select 1
          from CatalogMovementLine l
          where l.tenantId = :tenantId
            and l.movementId = m.id
            and (:metricType is null or l.metricType = :metricType)
            and (:estoqueTipoId is null or l.estoqueTipoId = :estoqueTipoId)
            and (:filialId is null or l.filialId = :filialId)
        )
      )
    """)
  Page<CatalogMovement> search(
    @Param("tenantId") Long tenantId,
    @Param("catalogType") CatalogConfigurationType catalogType,
    @Param("catalogoId") Long catalogoId,
    @Param("agrupadorEmpresaId") Long agrupadorEmpresaId,
    @Param("origemTipo") CatalogMovementOriginType origemTipo,
    @Param("fromDate") Instant fromDate,
    @Param("toDate") Instant toDate,
    @Param("metricType") CatalogMovementMetricType metricType,
    @Param("estoqueTipoId") Long estoqueTipoId,
    @Param("filialId") Long filialId,
    Pageable pageable);
}
