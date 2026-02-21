package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.Locatario;
import com.ia.app.domain.TenantUnit;
import com.ia.app.repository.LocatarioRepository;
import com.ia.app.repository.OfficialUnitRepository;
import com.ia.app.repository.TenantUnitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@DataJpaTest
@Import({
  AuditingConfig.class,
  OfficialUnitSeedService.class,
  TenantUnitMirrorService.class,
  OfficialUnitSeedServiceTest.JacksonConfig.class
})
class OfficialUnitSeedServiceTest {

  @Autowired
  private OfficialUnitSeedService seedService;

  @Autowired
  private TenantUnitMirrorService mirrorService;

  @Autowired
  private OfficialUnitRepository officialUnitRepository;

  @Autowired
  private TenantUnitRepository tenantUnitRepository;

  @Autowired
  private LocatarioRepository locatarioRepository;

  @Test
  void shouldSeedOfficialUnitsIdempotently() {
    long countBefore = officialUnitRepository.count();
    OfficialUnitSeedService.SeedResult first = seedService.seedOfficialUnitsFromResource();
    long countAfterFirst = officialUnitRepository.count();

    OfficialUnitSeedService.SeedResult second = seedService.seedOfficialUnitsFromResource();
    long countAfterSecond = officialUnitRepository.count();

    if (countBefore == 0) {
      assertThat(first.inserted()).isGreaterThan(0);
    }
    assertThat(countAfterFirst).isGreaterThan(0);
    assertThat(second.inserted()).isZero();
    assertThat(countAfterSecond).isEqualTo(countAfterFirst);
  }

  @Test
  void shouldCreateMirrorTenantUnitsForNewTenant() {
    seedService.seedOfficialUnitsFromResource();

    Locatario tenant = new Locatario();
    tenant.setNome("Tenant mirror");
    tenant.setDataLimiteAcesso(LocalDate.now().plusDays(30));
    tenant.setAtivo(true);
    tenant = locatarioRepository.saveAndFlush(tenant);

    int created = mirrorService.seedMissingMirrorsForTenant(tenant.getId());
    long officialCount = officialUnitRepository.count();
    List<TenantUnit> mirrors = tenantUnitRepository.findAllByTenantIdOrderBySiglaAsc(tenant.getId());

    assertThat(created).isEqualTo((int) officialCount);
    assertThat(mirrors).hasSize((int) officialCount);
    assertThat(mirrors).allSatisfy(unit -> {
      assertThat(unit.isSystemMirror()).isTrue();
      assertThat(unit.getFatorParaOficial()).isEqualByComparingTo(BigDecimal.ONE.setScale(UnitConversionService.FACTOR_SCALE));
    });

    int reconcileCreated = mirrorService.seedMissingMirrorsForTenant(tenant.getId());
    assertThat(reconcileCreated).isZero();
  }

  @TestConfiguration
  static class JacksonConfig {
    @Bean
    @Primary
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }
  }
}
