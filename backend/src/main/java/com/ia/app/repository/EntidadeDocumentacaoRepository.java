package com.ia.app.repository;

import com.ia.app.domain.EntidadeDocumentacao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeDocumentacaoRepository extends JpaRepository<EntidadeDocumentacao, Long> {
  Optional<EntidadeDocumentacao> findByTenantIdAndEmpresaIdAndRegistroEntidadeId(
    Long tenantId, Long empresaId, Long registroEntidadeId);
}
