package com.ia.app.repository;

import com.ia.app.domain.ConfigFormulario;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfigFormularioRepository extends JpaRepository<ConfigFormulario, Long> {
  boolean existsByIdAndTenantId(Long id, Long tenantId);
  Optional<ConfigFormulario> findFirstByTenantIdAndScreenIdAndScopeTipoAndScopeValor(Long tenantId, String screenId, String scopeTipo, String scopeValor);
  Optional<ConfigFormulario> findFirstByTenantIdAndScreenIdAndScopeTipoAndScopeValorIn(Long tenantId, String screenId, String scopeTipo, List<String> scopeValor);
  Optional<ConfigFormulario> findFirstByTenantIdAndScreenIdAndScopeTipo(Long tenantId, String screenId, String scopeTipo);

  @Query("select max(c.updatedAt) from ConfigFormulario c where c.tenantId = :tenantId and c.screenId = :screenId")
  Instant findMaxUpdatedAt(@Param("tenantId") Long tenantId, @Param("screenId") String screenId);
}
