package com.ia.app.repository;

import com.ia.app.domain.MovimentoEstoqueItem;
import com.ia.app.domain.CatalogConfigurationType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface MovimentoEstoqueItemRepository extends JpaRepository<MovimentoEstoqueItem, Long> {

  List<MovimentoEstoqueItem> findAllByTenantIdAndMovimentoEstoqueIdOrderByOrdemAscIdAsc(Long tenantId, Long movimentoEstoqueId);

  void deleteAllByTenantIdAndMovimentoEstoqueId(Long tenantId, Long movimentoEstoqueId);

  Optional<MovimentoEstoqueItem> findByIdAndTenantId(Long id, Long tenantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<MovimentoEstoqueItem> findWithLockByIdAndTenantId(Long id, Long tenantId);

  Optional<MovimentoEstoqueItem> findTopByTenantIdAndMovimentoEstoqueIdOrderByCodigoDescIdDesc(
    Long tenantId,
    Long movimentoEstoqueId);

  boolean existsByTenantIdAndCatalogTypeAndCatalogItemIdAndEstoqueMovimentadoTrue(
    Long tenantId,
    CatalogConfigurationType catalogType,
    Long catalogItemId);
}
