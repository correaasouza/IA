package com.ia.app.repository;

import com.ia.app.domain.MovimentoItemTipo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MovimentoItemTipoRepository
    extends JpaRepository<MovimentoItemTipo, Long>, JpaSpecificationExecutor<MovimentoItemTipo> {

  Optional<MovimentoItemTipo> findByIdAndTenantId(Long id, Long tenantId);

  boolean existsByTenantIdAndNomeIgnoreCaseAndIdNot(Long tenantId, String nome, Long id);

  boolean existsByTenantIdAndNomeIgnoreCase(Long tenantId, String nome);
}
