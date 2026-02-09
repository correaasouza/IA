package com.ia.app.repository;

import com.ia.app.domain.EntidadeDefinicao;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeDefinicaoRepository extends JpaRepository<EntidadeDefinicao, Long> {
  Page<EntidadeDefinicao> findAllByTenantId(Long tenantId, Pageable pageable);
  Optional<EntidadeDefinicao> findByIdAndTenantId(Long id, Long tenantId);
  Optional<EntidadeDefinicao> findByTenantIdAndCodigo(Long tenantId, String codigo);
}
