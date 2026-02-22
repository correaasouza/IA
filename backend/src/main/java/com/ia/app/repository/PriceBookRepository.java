package com.ia.app.repository;

import com.ia.app.domain.PriceBook;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceBookRepository extends JpaRepository<PriceBook, Long> {

  List<PriceBook> findAllByTenantIdOrderByNameAsc(Long tenantId);

  Optional<PriceBook> findByIdAndTenantId(Long id, Long tenantId);

  Optional<PriceBook> findByTenantIdAndDefaultBookTrue(Long tenantId);

  Optional<PriceBook> findByTenantIdAndNameIgnoreCase(Long tenantId, String name);

  boolean existsByTenantIdAndId(Long tenantId, Long id);
}
