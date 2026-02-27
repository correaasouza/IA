package com.ia.app.repository;

import com.ia.app.domain.UsuarioEmpresaPreferencia;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioEmpresaPreferenciaRepository extends JpaRepository<UsuarioEmpresaPreferencia, Long> {
  Optional<UsuarioEmpresaPreferencia> findByTenantIdAndUsuarioId(Long tenantId, String usuarioId);
  void deleteByTenantIdAndUsuarioId(Long tenantId, String usuarioId);
  void deleteAllByUsuarioId(String usuarioId);
}
