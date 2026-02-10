package com.ia.app.repository;

import com.ia.app.domain.PessoaContato;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaContatoRepository extends JpaRepository<PessoaContato, Long> {
  List<PessoaContato> findAllByTenantIdAndPessoaId(Long tenantId, Long pessoaId);
  Optional<PessoaContato> findByIdAndTenantId(Long id, Long tenantId);
  void deleteAllByTenantIdAndPessoaId(Long tenantId, Long pessoaId);
}
