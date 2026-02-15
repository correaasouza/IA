package com.ia.app.repository;

import com.ia.app.domain.Pessoa;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {
  Page<Pessoa> findAllByTenantId(Long tenantId, Pageable pageable);
  java.util.List<Pessoa> findAllByTenantIdAndIdIn(Long tenantId, Collection<Long> ids);
  Optional<Pessoa> findByIdAndTenantId(Long id, Long tenantId);
  Optional<Pessoa> findByTenantIdAndCpf(Long tenantId, String cpf);
  Optional<Pessoa> findByTenantIdAndCnpj(Long tenantId, String cnpj);
  Optional<Pessoa> findByTenantIdAndIdEstrangeiro(Long tenantId, String idEstrangeiro);
  Optional<Pessoa> findByTenantIdAndTipoRegistroAndRegistroFederalNormalizado(
    Long tenantId, String tipoRegistro, String registroFederalNormalizado);
}
