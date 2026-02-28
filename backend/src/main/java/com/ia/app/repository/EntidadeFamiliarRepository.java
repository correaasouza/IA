package com.ia.app.repository;

import com.ia.app.domain.EntidadeFamiliar;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeFamiliarRepository extends JpaRepository<EntidadeFamiliar, Long> {
  List<EntidadeFamiliar> findAllByTenantIdAndEmpresaIdAndRegistroEntidadeIdOrderByIdAsc(
    Long tenantId, Long empresaId, Long registroEntidadeId);

  Optional<EntidadeFamiliar> findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(
    Long id, Long tenantId, Long empresaId, Long registroEntidadeId);
}
