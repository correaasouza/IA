package com.ia.app.repository;

import com.ia.app.domain.EntidadeContato;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeContatoRepository extends JpaRepository<EntidadeContato, Long> {
  List<EntidadeContato> findAllByTenantIdAndEmpresaIdAndRegistroEntidadeIdOrderByIdAsc(
    Long tenantId, Long empresaId, Long registroEntidadeId);

  Optional<EntidadeContato> findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(
    Long id, Long tenantId, Long empresaId, Long registroEntidadeId);
}
