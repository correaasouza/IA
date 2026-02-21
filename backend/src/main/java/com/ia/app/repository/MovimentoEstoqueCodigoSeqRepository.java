package com.ia.app.repository;

import com.ia.app.domain.MovimentoEstoqueCodigoSeq;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface MovimentoEstoqueCodigoSeqRepository extends JpaRepository<MovimentoEstoqueCodigoSeq, Long> {

  Optional<MovimentoEstoqueCodigoSeq> findByTenantIdAndMovimentoConfigId(Long tenantId, Long movimentoConfigId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<MovimentoEstoqueCodigoSeq> findWithLockByTenantIdAndMovimentoConfigId(Long tenantId, Long movimentoConfigId);
}
