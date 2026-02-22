package com.ia.app.repository;

import com.ia.app.domain.PriceVariant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceVariantRepository extends JpaRepository<PriceVariant, Long> {

  List<PriceVariant> findAllByTenantIdOrderByNameAsc(Long tenantId);

  Optional<PriceVariant> findByIdAndTenantId(Long id, Long tenantId);

  Optional<PriceVariant> findByTenantIdAndNameIgnoreCase(Long tenantId, String name);

  boolean existsByTenantIdAndId(Long tenantId, Long id);
}
