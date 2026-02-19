package com.ia.app.repository;

import com.ia.app.domain.MovimentoConfigItemTipo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentoConfigItemTipoRepository extends JpaRepository<MovimentoConfigItemTipo, Long> {

  List<MovimentoConfigItemTipo> findAllByTenantIdAndMovimentoConfigIdOrderByIdAsc(Long tenantId, Long movimentoConfigId);

  Optional<MovimentoConfigItemTipo> findByTenantIdAndMovimentoConfigIdAndMovimentoItemTipoId(
    Long tenantId,
    Long movimentoConfigId,
    Long movimentoItemTipoId);
}
