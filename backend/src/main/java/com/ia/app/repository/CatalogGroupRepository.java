package com.ia.app.repository;

import com.ia.app.domain.CatalogGroup;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogGroupRepository extends JpaRepository<CatalogGroup, Long> {
  Optional<CatalogGroup> findByIdAndTenantIdAndCatalogConfigurationIdAndAtivoTrue(
    Long id, Long tenantId, Long catalogConfigurationId);

  List<CatalogGroup> findAllByTenantIdAndCatalogConfigurationIdAndParentIdIsNullAndAtivoTrueOrderByOrdemAscNomeAsc(
    Long tenantId, Long catalogConfigurationId);

  List<CatalogGroup> findAllByTenantIdAndCatalogConfigurationIdAndParentIdAndAtivoTrueOrderByOrdemAscNomeAsc(
    Long tenantId, Long catalogConfigurationId, Long parentId);

  List<CatalogGroup> findAllByTenantIdAndCatalogConfigurationIdAndAtivoTrueOrderByPathAsc(
    Long tenantId, Long catalogConfigurationId);

  List<CatalogGroup> findAllByTenantIdAndCatalogConfigurationIdAndPathStartingWithAndAtivoTrueOrderByPathAsc(
    Long tenantId, Long catalogConfigurationId, String pathPrefix);

  List<CatalogGroup> findAllByTenantIdAndCatalogConfigurationIdAndIdIn(
    Long tenantId, Long catalogConfigurationId, Collection<Long> ids);

  boolean existsByTenantIdAndCatalogConfigurationIdAndParentIdAndNomeNormalizadoAndAtivoTrue(
    Long tenantId, Long catalogConfigurationId, Long parentId, String nomeNormalizado);

  boolean existsByTenantIdAndCatalogConfigurationIdAndParentIdAndNomeNormalizadoAndAtivoTrueAndIdNot(
    Long tenantId, Long catalogConfigurationId, Long parentId, String nomeNormalizado, Long id);

  boolean existsByTenantIdAndCatalogConfigurationIdAndParentIdAndAtivoTrue(
    Long tenantId, Long catalogConfigurationId, Long parentId);

  long countByTenantIdAndCatalogConfigurationIdAndParentIdAndAtivoTrue(
    Long tenantId, Long catalogConfigurationId, Long parentId);
}
