package com.ia.app.repository;

import com.ia.app.domain.MovimentoEstoque;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface MovimentoEstoqueRepository
    extends JpaRepository<MovimentoEstoque, Long>, JpaSpecificationExecutor<MovimentoEstoque> {

  Optional<MovimentoEstoque> findByIdAndTenantId(Long id, Long tenantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<MovimentoEstoque> findWithLockByIdAndTenantId(Long id, Long tenantId);
}
