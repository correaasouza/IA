package com.ia.app.repository;

import com.ia.app.domain.EntidadeRegistro;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntidadeRegistroRepository extends JpaRepository<EntidadeRegistro, Long> {
  Page<EntidadeRegistro> findAllByTenantIdAndEntidadeDefinicaoId(Long tenantId, Long entidadeDefinicaoId, Pageable pageable);
  Optional<EntidadeRegistro> findByIdAndTenantId(Long id, Long tenantId);
  Page<EntidadeRegistro> findAllByTenantIdAndEntidadeDefinicaoIdAndNomeContainingIgnoreCase(Long tenantId, Long entidadeDefinicaoId, String nome, Pageable pageable);
  Page<EntidadeRegistro> findAllByTenantIdAndEntidadeDefinicaoIdAndCpfCnpjContainingIgnoreCase(Long tenantId, Long entidadeDefinicaoId, String cpfCnpj, Pageable pageable);
  Page<EntidadeRegistro> findAllByTenantIdAndEntidadeDefinicaoIdAndAtivo(Long tenantId, Long entidadeDefinicaoId, boolean ativo, Pageable pageable);

  @Query("select r.entidadeDefinicaoId, count(r.id) from EntidadeRegistro r where r.tenantId = :tenantId group by r.entidadeDefinicaoId")
  List<Object[]> countByEntidade(@Param("tenantId") Long tenantId);

  @Query("select r.entidadeDefinicaoId, count(r.id) from EntidadeRegistro r " +
         "where r.tenantId = :tenantId " +
         "and (:entidadeId is null or r.entidadeDefinicaoId = :entidadeId) " +
         "and (:de is null or r.createdAt >= :de) " +
         "and (:ate is null or r.createdAt <= :ate) " +
         "group by r.entidadeDefinicaoId")
  List<Object[]> countByEntidadeFiltered(
      @Param("tenantId") Long tenantId,
      @Param("entidadeId") Long entidadeId,
      @Param("de") java.time.Instant de,
      @Param("ate") java.time.Instant ate);

  @Query("select r from EntidadeRegistro r where r.tenantId = :tenantId and r.ativo = true")
  List<EntidadeRegistro> findAllAtivos(@Param("tenantId") Long tenantId);
}
