package com.ia.app.repository;

import com.ia.app.domain.MovimentoEstoqueItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentoEstoqueItemRepository extends JpaRepository<MovimentoEstoqueItem, Long> {

  List<MovimentoEstoqueItem> findAllByTenantIdAndMovimentoEstoqueIdOrderByOrdemAscIdAsc(Long tenantId, Long movimentoEstoqueId);

  void deleteAllByTenantIdAndMovimentoEstoqueId(Long tenantId, Long movimentoEstoqueId);
}
