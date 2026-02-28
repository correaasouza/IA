package com.ia.app.repository;

import com.ia.app.domain.EntidadeInfoComercial;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeInfoComercialRepository extends JpaRepository<EntidadeInfoComercial, Long> {
  Optional<EntidadeInfoComercial> findByTenantIdAndEmpresaIdAndRegistroEntidadeId(
    Long tenantId, Long empresaId, Long registroEntidadeId);
}
