package com.ia.app.repository;

import com.ia.app.domain.AgrupadorEmpresaItem;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AgrupadorEmpresaItemRepository extends JpaRepository<AgrupadorEmpresaItem, Long> {
  Optional<AgrupadorEmpresaItem> findByTenantIdAndConfigTypeAndConfigIdAndAgrupadorIdAndEmpresaId(
    Long tenantId, String configType, Long configId, Long agrupadorId, Long empresaId);

  Optional<AgrupadorEmpresaItem> findByTenantIdAndConfigTypeAndConfigIdAndEmpresaId(
    Long tenantId, String configType, Long configId, Long empresaId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<AgrupadorEmpresaItem> findWithLockByTenantIdAndConfigTypeAndConfigIdAndEmpresaId(
    Long tenantId, String configType, Long configId, Long empresaId);
}
