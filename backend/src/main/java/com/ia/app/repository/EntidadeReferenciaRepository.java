package com.ia.app.repository;

import com.ia.app.domain.EntidadeReferencia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeReferenciaRepository extends JpaRepository<EntidadeReferencia, Long> {
  List<EntidadeReferencia> findAllByTenantIdAndEmpresaIdAndRegistroEntidadeIdOrderByIdAsc(
    Long tenantId, Long empresaId, Long registroEntidadeId);

  Optional<EntidadeReferencia> findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(
    Long id, Long tenantId, Long empresaId, Long registroEntidadeId);
}
