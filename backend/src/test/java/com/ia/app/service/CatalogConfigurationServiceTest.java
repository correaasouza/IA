package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.CatalogConfiguration;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogNumberingMode;
import com.ia.app.dto.CatalogConfigurationResponse;
import com.ia.app.repository.CatalogConfigurationRepository;
import com.ia.app.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({
  AuditingConfig.class,
  CatalogConfigurationService.class,
  AuditService.class
})
class CatalogConfigurationServiceTest {

  @Autowired
  private CatalogConfigurationService service;

  @Autowired
  private CatalogConfigurationRepository repository;

  @AfterEach
  void cleanTenantContext() {
    TenantContext.clear();
  }

  @Test
  void shouldLazyCreateConfigurationOnGet() {
    TenantContext.setTenantId(31L);

    CatalogConfigurationResponse created = service.getOrCreate(CatalogConfigurationType.PRODUCTS);
    CatalogConfigurationResponse loaded = service.getOrCreate(CatalogConfigurationType.PRODUCTS);

    assertThat(created.id()).isNotNull();
    assertThat(created.numberingMode()).isEqualTo(CatalogNumberingMode.AUTOMATICA);
    assertThat(loaded.id()).isEqualTo(created.id());
    assertThat(repository.findByTenantIdAndType(31L, CatalogConfigurationType.PRODUCTS)).isPresent();
  }

  @Test
  void shouldEnforceUniquenessPerTenantAndType() {
    CatalogConfiguration first = new CatalogConfiguration();
    first.setTenantId(41L);
    first.setType(CatalogConfigurationType.SERVICES);
    first.setNumberingMode(CatalogNumberingMode.AUTOMATICA);
    first.setActive(true);
    repository.saveAndFlush(first);

    CatalogConfiguration duplicated = new CatalogConfiguration();
    duplicated.setTenantId(41L);
    duplicated.setType(CatalogConfigurationType.SERVICES);
    duplicated.setNumberingMode(CatalogNumberingMode.MANUAL);
    duplicated.setActive(true);

    assertThatThrownBy(() -> repository.saveAndFlush(duplicated))
      .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldUpdateNumberingMode() {
    TenantContext.setTenantId(51L);
    CatalogConfigurationResponse updated = service.update(CatalogConfigurationType.PRODUCTS, CatalogNumberingMode.MANUAL);

    assertThat(updated.numberingMode()).isEqualTo(CatalogNumberingMode.MANUAL);
    assertThat(repository.findByTenantIdAndType(51L, CatalogConfigurationType.PRODUCTS))
      .map(CatalogConfiguration::getNumberingMode)
      .contains(CatalogNumberingMode.MANUAL);
  }

  @Test
  void shouldIsolateConfigurationsByTenant() {
    TenantContext.setTenantId(61L);
    CatalogConfigurationResponse t1 = service.getOrCreate(CatalogConfigurationType.PRODUCTS);

    TenantContext.setTenantId(62L);
    CatalogConfigurationResponse t2 = service.getOrCreate(CatalogConfigurationType.PRODUCTS);

    assertThat(t1.id()).isNotEqualTo(t2.id());
    assertThat(repository.existsByTenantIdAndType(61L, CatalogConfigurationType.PRODUCTS)).isTrue();
    assertThat(repository.existsByTenantIdAndType(62L, CatalogConfigurationType.PRODUCTS)).isTrue();
  }
}
