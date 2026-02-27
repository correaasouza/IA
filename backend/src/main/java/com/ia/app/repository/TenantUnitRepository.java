package com.ia.app.repository;

import com.ia.app.domain.TenantUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantUnitRepository extends JpaRepository<TenantUnit, UUID> {

  List<TenantUnit> findAllByTenantIdOrderBySiglaAsc(Long tenantId);

  Optional<TenantUnit> findByIdAndTenantId(UUID id, Long tenantId);

  Optional<TenantUnit> findByTenantIdAndSiglaIgnoreCase(Long tenantId, String sigla);

  Optional<TenantUnit> findByTenantIdAndPadraoTrue(Long tenantId);

  Optional<TenantUnit> findByTenantIdAndUnidadeOficialIdAndSystemMirrorTrue(Long tenantId, UUID unidadeOficialId);

  List<TenantUnit> findAllByTenantIdAndUnidadeOficialIdIn(Long tenantId, Set<UUID> unidadeOficialIds);

  List<TenantUnit> findAllByTenantIdAndIdIn(Long tenantId, Set<UUID> ids);

  List<TenantUnit> findAllByTenantIdAndSystemMirrorTrueOrderBySiglaAsc(Long tenantId);

  List<TenantUnit> findAllByTenantIdAndUnidadeOficialIdOrderBySiglaAsc(Long tenantId, UUID unidadeOficialId);

  boolean existsByTenantIdAndSiglaIgnoreCase(Long tenantId, String sigla);

  boolean existsByTenantIdAndSiglaIgnoreCaseAndIdNot(Long tenantId, String sigla, UUID id);

  boolean existsByTenantIdAndPadraoTrue(Long tenantId);

  boolean existsByTenantIdAndId(Long tenantId, UUID id);

  long countByTenantIdAndUnidadeOficialId(Long tenantId, UUID unidadeOficialId);

  @Modifying
  @Query("update TenantUnit tu set tu.padrao = false where tu.tenantId = :tenantId and (:excludedId is null or tu.id <> :excludedId)")
  int clearDefaultByTenantId(@Param("tenantId") Long tenantId, @Param("excludedId") UUID excludedId);
}
