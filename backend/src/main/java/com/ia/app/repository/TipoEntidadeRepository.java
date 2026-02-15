package com.ia.app.repository;

import com.ia.app.domain.TipoEntidade;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TipoEntidadeRepository extends JpaRepository<TipoEntidade, Long>, JpaSpecificationExecutor<TipoEntidade> {
  Optional<TipoEntidade> findByIdAndTenantId(Long id, Long tenantId);

  Optional<TipoEntidade> findByTenantIdAndCodigoSeed(Long tenantId, String codigoSeed);

  boolean existsByTenantIdAndNomeIgnoreCaseAndAtivoTrue(Long tenantId, String nome);

  boolean existsByTenantIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(Long tenantId, String nome, Long id);

  boolean existsByIdAndTenantIdAndAtivoTrue(Long id, Long tenantId);
}
