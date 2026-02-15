package com.ia.app.repository;

import com.ia.app.domain.TipoEntidadeConfigPorAgrupador;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoEntidadeConfigPorAgrupadorRepository extends JpaRepository<TipoEntidadeConfigPorAgrupador, Long> {
  List<TipoEntidadeConfigPorAgrupador> findAllByTenantIdAndTipoEntidadeIdAndAtivoTrue(Long tenantId, Long tipoEntidadeId);

  Optional<TipoEntidadeConfigPorAgrupador> findByTenantIdAndTipoEntidadeIdAndAgrupadorIdAndAtivoTrue(
      Long tenantId, Long tipoEntidadeId, Long agrupadorId);
}
