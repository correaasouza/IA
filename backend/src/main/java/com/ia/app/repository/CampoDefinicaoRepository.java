package com.ia.app.repository;

import com.ia.app.domain.CampoDefinicao;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampoDefinicaoRepository extends JpaRepository<CampoDefinicao, Long> {
  Page<CampoDefinicao> findAllByTenantIdAndTipoEntidadeId(Long tenantId, Long tipoEntidadeId, Pageable pageable);
  Optional<CampoDefinicao> findByIdAndTenantId(Long id, Long tenantId);

  @Query("select max(c.updatedAt) from CampoDefinicao c where c.tenantId = :tenantId and c.tipoEntidadeId = :tipoEntidadeId")
  Instant findMaxUpdatedAt(@Param("tenantId") Long tenantId, @Param("tipoEntidadeId") Long tipoEntidadeId);
}
