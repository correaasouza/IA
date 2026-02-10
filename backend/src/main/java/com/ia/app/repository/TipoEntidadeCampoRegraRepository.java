package com.ia.app.repository;

import com.ia.app.domain.TipoEntidadeCampoRegra;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoEntidadeCampoRegraRepository extends JpaRepository<TipoEntidadeCampoRegra, Long> {
  List<TipoEntidadeCampoRegra> findAllByTenantIdAndTipoEntidadeId(Long tenantId, Long tipoEntidadeId);
  Optional<TipoEntidadeCampoRegra> findByTenantIdAndTipoEntidadeIdAndCampo(Long tenantId, Long tipoEntidadeId, String campo);
}
