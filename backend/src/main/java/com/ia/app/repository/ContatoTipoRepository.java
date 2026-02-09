package com.ia.app.repository;

import com.ia.app.domain.ContatoTipo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContatoTipoRepository extends JpaRepository<ContatoTipo, Long> {
  List<ContatoTipo> findAllByTenantId(Long tenantId);
  Optional<ContatoTipo> findByIdAndTenantId(Long id, Long tenantId);
  Optional<ContatoTipo> findByTenantIdAndCodigo(Long tenantId, String codigo);
}
