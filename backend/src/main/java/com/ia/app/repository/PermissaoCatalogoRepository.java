package com.ia.app.repository;

import com.ia.app.domain.PermissaoCatalogo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissaoCatalogoRepository extends JpaRepository<PermissaoCatalogo, Long> {
  List<PermissaoCatalogo> findAllByTenantIdOrderByCodigo(Long tenantId);
  Optional<PermissaoCatalogo> findByTenantIdAndCodigo(Long tenantId, String codigo);
  List<PermissaoCatalogo> findAllByTenantIdAndAtivoTrue(Long tenantId);
}
