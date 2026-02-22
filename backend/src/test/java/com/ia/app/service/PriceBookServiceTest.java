package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.config.AuditingConfig;
import com.ia.app.repository.PriceBookRepository;
import com.ia.app.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
  AuditingConfig.class,
  PriceBookService.class
})
class PriceBookServiceTest {

  @Autowired
  private PriceBookService service;

  @Autowired
  private PriceBookRepository repository;

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void shouldCreateDefaultBookWhenListingWithoutExistingRows() {
    TenantContext.setTenantId(801L);

    assertThat(repository.findAllByTenantIdOrderByNameAsc(801L)).isEmpty();

    var rows = service.list();

    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).defaultBook()).isTrue();
    assertThat(rows.get(0).active()).isTrue();
    assertThat(rows.get(0).name()).isEqualTo("Padrao");
    assertThat(repository.findAllByTenantIdOrderByNameAsc(801L)).hasSize(1);
  }

  @Test
  void shouldNotDuplicateDefaultBookAcrossMultipleListCalls() {
    TenantContext.setTenantId(802L);

    service.list();
    service.list();

    assertThat(repository.findAllByTenantIdOrderByNameAsc(802L)).hasSize(1);
  }
}
