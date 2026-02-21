package com.ia.app.repository;

import com.ia.app.domain.TenantUnitConversion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantUnitConversionRepository extends JpaRepository<TenantUnitConversion, UUID> {

  List<TenantUnitConversion> findAllByTenantIdOrderByUnidadeOrigemIdAscUnidadeDestinoIdAsc(Long tenantId);

  Optional<TenantUnitConversion> findByIdAndTenantId(UUID id, Long tenantId);

  Optional<TenantUnitConversion> findByTenantIdAndUnidadeOrigemIdAndUnidadeDestinoId(
    Long tenantId,
    UUID unidadeOrigemId,
    UUID unidadeDestinoId);

  List<TenantUnitConversion> findAllByTenantIdAndUnidadeOrigemId(Long tenantId, UUID unidadeOrigemId);

  List<TenantUnitConversion> findAllByTenantIdAndUnidadeDestinoId(Long tenantId, UUID unidadeDestinoId);

  boolean existsByTenantIdAndUnidadeOrigemIdAndUnidadeDestinoId(
    Long tenantId,
    UUID unidadeOrigemId,
    UUID unidadeDestinoId);

  boolean existsByTenantIdAndUnidadeOrigemIdAndUnidadeDestinoIdAndIdNot(
    Long tenantId,
    UUID unidadeOrigemId,
    UUID unidadeDestinoId,
    UUID id);

  void deleteByTenantIdAndUnidadeOrigemIdAndUnidadeDestinoId(
    Long tenantId,
    UUID unidadeOrigemId,
    UUID unidadeDestinoId);
}
