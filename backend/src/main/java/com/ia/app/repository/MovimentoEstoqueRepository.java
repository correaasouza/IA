package com.ia.app.repository;

import com.ia.app.domain.MovimentoEstoque;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MovimentoEstoqueRepository
    extends JpaRepository<MovimentoEstoque, Long>, JpaSpecificationExecutor<MovimentoEstoque> {

  Optional<MovimentoEstoque> findByIdAndTenantId(Long id, Long tenantId);
}
