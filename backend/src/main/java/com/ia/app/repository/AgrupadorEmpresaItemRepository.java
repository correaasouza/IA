package com.ia.app.repository;

import com.ia.app.domain.AgrupadorEmpresaItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgrupadorEmpresaItemRepository extends JpaRepository<AgrupadorEmpresaItem, Long> {
  Optional<AgrupadorEmpresaItem> findByTenantIdAndConfigTypeAndConfigIdAndAgrupadorIdAndEmpresaId(
    Long tenantId, String configType, Long configId, Long agrupadorId, Long empresaId);

  Optional<AgrupadorEmpresaItem> findByTenantIdAndConfigTypeAndConfigIdAndEmpresaId(
    Long tenantId, String configType, Long configId, Long empresaId);
}
