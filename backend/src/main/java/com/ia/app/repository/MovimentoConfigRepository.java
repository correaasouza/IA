package com.ia.app.repository;

import com.ia.app.domain.MovimentoConfig;
import com.ia.app.domain.MovimentoTipo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimentoConfigRepository extends JpaRepository<MovimentoConfig, Long> {

  Page<MovimentoConfig> findAllByTenantIdAndTipoMovimentoOrderByUpdatedAtDescIdDesc(
    Long tenantId,
    MovimentoTipo tipoMovimento,
    Pageable pageable);

  Optional<MovimentoConfig> findByIdAndTenantId(Long id, Long tenantId);

  boolean existsByTenantIdAndTipoMovimento(Long tenantId, MovimentoTipo tipoMovimento);

  @Query("""
    select case when count(c.id) > 0 then true else false end
    from MovimentoConfig c
    join c.empresas e
    where c.tenantId = :tenantId
      and c.ativo = true
      and c.tipoMovimento = :tipoMovimento
      and c.contextoKey is null
      and e.empresaId = :empresaId
    """)
  boolean existsActiveGlobalByTenantAndTipoAndEmpresa(
    @Param("tenantId") Long tenantId,
    @Param("tipoMovimento") MovimentoTipo tipoMovimento,
    @Param("empresaId") Long empresaId);

  @Query("""
    select count(distinct c.id)
    from MovimentoConfig c
    join c.empresas e
    where c.tenantId = :tenantId
      and c.ativo = true
      and c.tipoMovimento = :tipoMovimento
      and ((:contextoKey is null and c.contextoKey is null) or c.contextoKey = :contextoKey)
      and e.empresaId in :empresaIds
      and (:ignoreId is null or c.id <> :ignoreId)
    """)
  long countActiveConflicts(
    @Param("tenantId") Long tenantId,
    @Param("tipoMovimento") MovimentoTipo tipoMovimento,
    @Param("contextoKey") String contextoKey,
    @Param("empresaIds") Collection<Long> empresaIds,
    @Param("ignoreId") Long ignoreId);

  @Query("""
    select c
    from MovimentoConfig c
    join c.empresas e
    where c.tenantId = :tenantId
      and c.ativo = true
      and c.tipoMovimento = :tipoMovimento
      and e.empresaId = :empresaId
      and (
        (:contextoInformado = false and c.contextoKey is null)
        or (:contextoInformado = true and (c.contextoKey = :contextoKey or c.contextoKey is null))
      )
    order by case when c.contextoKey is null then 0 else 1 end desc,
      c.updatedAt desc,
      c.id desc
    """)
  List<MovimentoConfig> findCandidatesForResolver(
    @Param("tenantId") Long tenantId,
    @Param("tipoMovimento") MovimentoTipo tipoMovimento,
    @Param("empresaId") Long empresaId,
    @Param("contextoKey") String contextoKey,
    @Param("contextoInformado") boolean contextoInformado);

  @Query("""
    select distinct c.tipoMovimento
    from MovimentoConfig c
    join c.empresas e
    where c.tenantId = :tenantId
      and c.ativo = true
      and e.empresaId = :empresaId
    """)
  List<MovimentoTipo> findTiposAtivosByTenantAndEmpresa(
    @Param("tenantId") Long tenantId,
    @Param("empresaId") Long empresaId);
}
