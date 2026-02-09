package com.ia.app.repository;

import com.ia.app.domain.RegistroEntidade;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroEntidadeRepository extends JpaRepository<RegistroEntidade, Long> {
  Page<RegistroEntidade> findAllByTenantIdAndTipoEntidadeId(Long tenantId, Long tipoEntidadeId, Pageable pageable);
  Optional<RegistroEntidade> findByIdAndTenantId(Long id, Long tenantId);
}
