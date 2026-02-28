package com.ia.app.repository;

import com.ia.app.domain.EntidadeFormGroupConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadeFormGroupConfigRepository extends JpaRepository<EntidadeFormGroupConfig, Long> {
  List<EntidadeFormGroupConfig> findAllByTenantIdAndTipoEntidadeConfigAgrupadorIdOrderByOrdemAscIdAsc(
    Long tenantId,
    Long tipoEntidadeConfigAgrupadorId);

  Optional<EntidadeFormGroupConfig> findByTenantIdAndTipoEntidadeConfigAgrupadorIdAndGroupKey(
    Long tenantId,
    Long tipoEntidadeConfigAgrupadorId,
    String groupKey);
}

