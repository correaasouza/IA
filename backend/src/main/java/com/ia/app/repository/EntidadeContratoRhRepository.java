package com.ia.app.repository;

import com.ia.app.domain.EntidadeContratoRh;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeContratoRhRepository extends JpaRepository<EntidadeContratoRh, Long> {
  Optional<EntidadeContratoRh> findByTenantIdAndEmpresaIdAndRegistroEntidadeId(
    Long tenantId, Long empresaId, Long registroEntidadeId);
}
