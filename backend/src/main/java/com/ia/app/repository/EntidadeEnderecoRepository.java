package com.ia.app.repository;

import com.ia.app.domain.EntidadeEndereco;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeEnderecoRepository extends JpaRepository<EntidadeEndereco, Long> {
  List<EntidadeEndereco> findAllByTenantIdAndEmpresaIdAndRegistroEntidadeIdOrderByIdAsc(
    Long tenantId, Long empresaId, Long registroEntidadeId);

  Optional<EntidadeEndereco> findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(
    Long id, Long tenantId, Long empresaId, Long registroEntidadeId);
}
