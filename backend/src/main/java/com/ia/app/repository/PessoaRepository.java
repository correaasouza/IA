package com.ia.app.repository;

import com.ia.app.domain.Pessoa;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {
  Page<Pessoa> findAllByTenantId(Long tenantId, Pageable pageable);
  Optional<Pessoa> findByIdAndTenantId(Long id, Long tenantId);
  Optional<Pessoa> findByTenantIdAndCpf(Long tenantId, String cpf);
  Optional<Pessoa> findByTenantIdAndCnpj(Long tenantId, String cnpj);
  Optional<Pessoa> findByTenantIdAndIdEstrangeiro(Long tenantId, String idEstrangeiro);
}
