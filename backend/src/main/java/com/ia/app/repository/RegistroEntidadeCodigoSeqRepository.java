package com.ia.app.repository;

import com.ia.app.domain.RegistroEntidadeCodigoSeq;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface RegistroEntidadeCodigoSeqRepository extends JpaRepository<RegistroEntidadeCodigoSeq, Long> {
  Optional<RegistroEntidadeCodigoSeq> findByTenantIdAndTipoEntidadeConfigAgrupadorId(
    Long tenantId, Long tipoEntidadeConfigAgrupadorId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RegistroEntidadeCodigoSeq> findWithLockByTenantIdAndTipoEntidadeConfigAgrupadorId(
    Long tenantId, Long tipoEntidadeConfigAgrupadorId);
}
