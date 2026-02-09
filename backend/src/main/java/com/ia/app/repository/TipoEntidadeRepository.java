package com.ia.app.repository;

import com.ia.app.domain.TipoEntidade;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TipoEntidadeRepository extends JpaRepository<TipoEntidade, Long> {
  Page<TipoEntidade> findAllByTenantId(Long tenantId, Pageable pageable);
  Optional<TipoEntidade> findByIdAndTenantId(Long id, Long tenantId);

  @Query("select max(t.updatedAt) from TipoEntidade t where t.tenantId = :tenantId")
  Instant findMaxUpdatedAt(@Param("tenantId") Long tenantId);
}
