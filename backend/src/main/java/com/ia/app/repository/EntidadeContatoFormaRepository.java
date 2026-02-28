package com.ia.app.repository;

import com.ia.app.domain.EntidadeContatoForma;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeContatoFormaRepository extends JpaRepository<EntidadeContatoForma, Long> {
  List<EntidadeContatoForma> findAllByTenantIdAndEmpresaIdAndRegistroEntidadeIdAndContatoIdOrderByIdAsc(
    Long tenantId, Long empresaId, Long registroEntidadeId, Long contatoId);

  Optional<EntidadeContatoForma> findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeIdAndContatoId(
    Long id, Long tenantId, Long empresaId, Long registroEntidadeId, Long contatoId);
}
