package com.ia.app.repository;

import com.ia.app.domain.EntidadeInfoRh;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeInfoRhRepository extends JpaRepository<EntidadeInfoRh, Long> {
  Optional<EntidadeInfoRh> findByTenantIdAndEmpresaIdAndRegistroEntidadeId(
    Long tenantId, Long empresaId, Long registroEntidadeId);
}
