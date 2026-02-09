package com.ia.app.repository;

import com.ia.app.domain.ConfigColuna;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfigColunaRepository extends JpaRepository<ConfigColuna, Long> {
  Optional<ConfigColuna> findFirstByTenantIdAndScreenIdAndScopeTipoAndScopeValor(Long tenantId, String screenId, String scopeTipo, String scopeValor);
  Optional<ConfigColuna> findFirstByTenantIdAndScreenIdAndScopeTipoAndScopeValorIn(Long tenantId, String screenId, String scopeTipo, List<String> scopeValor);
  Optional<ConfigColuna> findFirstByTenantIdAndScreenIdAndScopeTipo(Long tenantId, String screenId, String scopeTipo);

  @Query("select max(c.updatedAt) from ConfigColuna c where c.tenantId = :tenantId and c.screenId = :screenId")
  Instant findMaxUpdatedAt(@Param("tenantId") Long tenantId, @Param("screenId") String screenId);
}
