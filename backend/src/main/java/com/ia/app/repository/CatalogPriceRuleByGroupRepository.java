package com.ia.app.repository;

import com.ia.app.domain.CatalogPriceRuleByGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogPriceRuleByGroupRepository extends JpaRepository<CatalogPriceRuleByGroup, Long> {

  List<CatalogPriceRuleByGroup> findAllByTenantIdAndCatalogConfigurationByGroupIdAndActiveTrueOrderByPriceTypeAsc(
    Long tenantId,
    Long catalogConfigurationByGroupId);
}
