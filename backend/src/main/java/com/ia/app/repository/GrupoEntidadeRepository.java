package com.ia.app.repository;

import com.ia.app.domain.GrupoEntidade;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoEntidadeRepository extends JpaRepository<GrupoEntidade, Long> {
  Optional<GrupoEntidade> findByIdAndTenantIdAndTipoEntidadeConfigAgrupadorIdAndAtivoTrue(
    Long id, Long tenantId, Long tipoEntidadeConfigAgrupadorId);

  List<GrupoEntidade> findAllByTenantIdAndTipoEntidadeConfigAgrupadorIdAndAtivoTrueOrderByPathAsc(
    Long tenantId, Long tipoEntidadeConfigAgrupadorId);

  List<GrupoEntidade> findAllByTenantIdAndTipoEntidadeConfigAgrupadorIdAndPathStartingWithAndAtivoTrueOrderByPathAsc(
    Long tenantId, Long tipoEntidadeConfigAgrupadorId, String pathPrefix);

  List<GrupoEntidade> findAllByTenantIdAndTipoEntidadeConfigAgrupadorIdAndIdIn(
    Long tenantId, Long tipoEntidadeConfigAgrupadorId, Collection<Long> ids);

  boolean existsByTenantIdAndTipoEntidadeConfigAgrupadorIdAndParentIdAndNomeNormalizadoAndAtivoTrue(
    Long tenantId, Long tipoEntidadeConfigAgrupadorId, Long parentId, String nomeNormalizado);

  boolean existsByTenantIdAndTipoEntidadeConfigAgrupadorIdAndParentIdAndNomeNormalizadoAndAtivoTrueAndIdNot(
    Long tenantId, Long tipoEntidadeConfigAgrupadorId, Long parentId, String nomeNormalizado, Long id);

  long countByTenantIdAndTipoEntidadeConfigAgrupadorIdAndParentIdAndAtivoTrue(
    Long tenantId, Long tipoEntidadeConfigAgrupadorId, Long parentId);
}
