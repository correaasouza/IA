package com.ia.app.repository;

import com.ia.app.domain.ContatoTipoPorEntidade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContatoTipoPorEntidadeRepository extends JpaRepository<ContatoTipoPorEntidade, Long> {
  List<ContatoTipoPorEntidade> findAllByTenantIdAndEntidadeDefinicaoId(Long tenantId, Long entidadeDefinicaoId);
  Optional<ContatoTipoPorEntidade> findByIdAndTenantId(Long id, Long tenantId);
  List<ContatoTipoPorEntidade> findAllByTenantId(Long tenantId);
}
