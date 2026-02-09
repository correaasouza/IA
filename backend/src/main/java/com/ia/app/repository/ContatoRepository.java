package com.ia.app.repository;

import com.ia.app.domain.Contato;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContatoRepository extends JpaRepository<Contato, Long> {
  List<Contato> findAllByTenantIdAndEntidadeRegistroId(Long tenantId, Long entidadeRegistroId);
  Optional<Contato> findByIdAndTenantId(Long id, Long tenantId);
  void deleteAllByTenantIdAndEntidadeRegistroId(Long tenantId, Long entidadeRegistroId);

  @Query("select c.tipo, count(c.id) from Contato c where c.tenantId = :tenantId group by c.tipo")
  List<Object[]> countByTipo(@Param("tenantId") Long tenantId);
}
