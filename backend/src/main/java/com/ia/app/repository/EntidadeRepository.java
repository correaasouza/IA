package com.ia.app.repository;

import com.ia.app.domain.Entidade;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntidadeRepository extends JpaRepository<Entidade, Long> {
  Page<Entidade> findAllByTenantId(Long tenantId, Pageable pageable);
  Page<Entidade> findAllByTenantIdAndTipoEntidadeId(Long tenantId, Long tipoEntidadeId, Pageable pageable);
  Optional<Entidade> findByIdAndTenantId(Long id, Long tenantId);

  @Query("select e.tipoEntidadeId, count(e.id) from Entidade e " +
         "where e.tenantId = :tenantId " +
         "group by e.tipoEntidadeId")
  List<Object[]> countByTipoEntidade(@Param("tenantId") Long tenantId);

  @Query("select e.tipoEntidadeId, count(e.id) from Entidade e " +
         "where e.tenantId = :tenantId " +
         "and (:tipoEntidadeId is null or e.tipoEntidadeId = :tipoEntidadeId) " +
         "and (:de is null or e.createdAt >= :de) " +
         "and (:ate is null or e.createdAt <= :ate) " +
         "group by e.tipoEntidadeId")
  List<Object[]> countByTipoEntidadeFiltered(
      @Param("tenantId") Long tenantId,
      @Param("tipoEntidadeId") Long tipoEntidadeId,
      @Param("de") Instant de,
      @Param("ate") Instant ate);

  @Query("select e from Entidade e where e.tenantId = :tenantId and e.ativo = true")
  List<Entidade> findAllAtivos(@Param("tenantId") Long tenantId);

  List<Entidade> findAllByTenantId(Long tenantId);
}
