package com.ia.app.repository;

import com.ia.app.domain.Entidade;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeRepository extends JpaRepository<Entidade, Long> {
  Page<Entidade> findAllByTenantId(Long tenantId, Pageable pageable);
  Page<Entidade> findAllByTenantIdAndTipoEntidadeId(Long tenantId, Long tipoEntidadeId, Pageable pageable);
  Optional<Entidade> findByIdAndTenantId(Long id, Long tenantId);
}
