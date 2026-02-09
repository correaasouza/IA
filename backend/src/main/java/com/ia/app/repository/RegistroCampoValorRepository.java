package com.ia.app.repository;

import com.ia.app.domain.RegistroCampoValor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroCampoValorRepository extends JpaRepository<RegistroCampoValor, Long> {
  List<RegistroCampoValor> findAllByTenantIdAndRegistroEntidadeIdIn(Long tenantId, List<Long> registroIds);
  List<RegistroCampoValor> findAllByTenantIdAndRegistroEntidadeId(Long tenantId, Long registroId);
}
