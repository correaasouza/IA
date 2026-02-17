package com.ia.app.repository;

import com.ia.app.domain.Empresa;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmpresaRepository extends JpaRepository<Empresa, Long>, JpaSpecificationExecutor<Empresa> {
  Optional<Empresa> findByIdAndTenantId(Long id, Long tenantId);
  List<Empresa> findAllByTenantIdAndIdIn(Long tenantId, Collection<Long> ids);
  List<Empresa> findAllByTenantIdAndAtivoTrueOrderByRazaoSocialAsc(Long tenantId);
  boolean existsByIdAndTenantId(Long id, Long tenantId);
  Optional<Empresa> findByTenantIdAndCnpj(Long tenantId, String cnpj);
  List<Empresa> findAllByTenantIdAndMatrizIdOrderByRazaoSocialAsc(Long tenantId, Long matrizId);
  boolean existsByTenantIdAndMatrizId(Long tenantId, Long matrizId);
}
