package com.ia.app.repository;

import com.ia.app.domain.EntidadeDadosFiscais;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeDadosFiscaisRepository extends JpaRepository<EntidadeDadosFiscais, Long> {
  Optional<EntidadeDadosFiscais> findByTenantIdAndEmpresaIdAndRegistroEntidadeId(
    Long tenantId, Long empresaId, Long registroEntidadeId);
}
