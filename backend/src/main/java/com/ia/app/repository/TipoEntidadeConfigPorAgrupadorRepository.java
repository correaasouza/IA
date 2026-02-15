package com.ia.app.repository;

import com.ia.app.domain.TipoEntidadeConfigPorAgrupador;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface TipoEntidadeConfigPorAgrupadorRepository extends JpaRepository<TipoEntidadeConfigPorAgrupador, Long> {
  List<TipoEntidadeConfigPorAgrupador> findAllByTenantIdAndTipoEntidadeIdAndAtivoTrue(Long tenantId, Long tipoEntidadeId);

  java.util.Optional<TipoEntidadeConfigPorAgrupador> findByIdAndTenantIdAndAtivoTrue(Long id, Long tenantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<TipoEntidadeConfigPorAgrupador> findWithLockByIdAndTenantIdAndAtivoTrue(Long id, Long tenantId);

  Optional<TipoEntidadeConfigPorAgrupador> findByTenantIdAndTipoEntidadeIdAndAgrupadorIdAndAtivoTrue(
      Long tenantId, Long tipoEntidadeId, Long agrupadorId);
}
