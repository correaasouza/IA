package com.ia.app.repository;

import com.ia.app.domain.EntidadeTratamento;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeTratamentoRepository extends JpaRepository<EntidadeTratamento, Long> {
  boolean existsByTenantIdAndIdAndAtivoTrue(Long tenantId, Long id);
  Optional<EntidadeTratamento> findByIdAndTenantId(Long id, Long tenantId);
}
