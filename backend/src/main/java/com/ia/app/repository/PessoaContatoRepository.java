package com.ia.app.repository;

import com.ia.app.domain.PessoaContato;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PessoaContatoRepository extends JpaRepository<PessoaContato, Long> {
  List<PessoaContato> findAllByTenantIdAndPessoaId(Long tenantId, Long pessoaId);
  List<PessoaContato> findAllByTenantIdAndPessoaIdIn(Long tenantId, Collection<Long> pessoaIds);
  Optional<PessoaContato> findByIdAndTenantId(Long id, Long tenantId);
  void deleteAllByTenantIdAndPessoaId(Long tenantId, Long pessoaId);

  @Query("select c.tipo, count(c.id) from PessoaContato c where c.tenantId = :tenantId group by c.tipo")
  List<Object[]> countByTipo(@Param("tenantId") Long tenantId);
}
