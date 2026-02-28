package com.ia.app.repository;

import com.ia.app.domain.EntidadeQualificacaoItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeQualificacaoItemRepository extends JpaRepository<EntidadeQualificacaoItem, Long> {
  List<EntidadeQualificacaoItem> findAllByTenantIdAndEmpresaIdAndRegistroEntidadeIdOrderByIdAsc(
    Long tenantId, Long empresaId, Long registroEntidadeId);

  Optional<EntidadeQualificacaoItem> findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(
    Long id, Long tenantId, Long empresaId, Long registroEntidadeId);
}
