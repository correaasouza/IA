package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.config.AuditingConfig;
import com.ia.app.dto.PessoaVinculoRequest;
import com.ia.app.repository.PessoaRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({AuditingConfig.class, PessoaResolveService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PessoaResolveServiceConcurrencyTest {

  private static final long TENANT_ID = 31L;

  @Autowired
  private PessoaResolveService service;

  @Autowired
  private PessoaRepository pessoaRepository;

  @Test
  void shouldCreateSinglePessoaWhenConcurrentRequestsUseSameLogicalKey() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    Callable<Long> c1 = () -> resolveWithSync(ready, start, "390.533.447-05");
    Callable<Long> c2 = () -> resolveWithSync(ready, start, "39053344705");

    Future<Long> f1 = pool.submit(c1);
    Future<Long> f2 = pool.submit(c2);

    assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
    start.countDown();

    List<Long> ids = new ArrayList<>();
    ids.add(f1.get(5, TimeUnit.SECONDS));
    ids.add(f2.get(5, TimeUnit.SECONDS));
    pool.shutdownNow();

    Set<Long> distinctIds = new HashSet<>(ids);
    assertThat(distinctIds).hasSize(1);

    long totalTenant = pessoaRepository.findAll().stream().filter(p -> TENANT_ID == p.getTenantId()).count();
    assertThat(totalTenant).isEqualTo(1);
  }

  private Long resolveWithSync(CountDownLatch ready, CountDownLatch start, String documento) throws Exception {
    ready.countDown();
    start.await(2, TimeUnit.SECONDS);
    return service.resolveOrCreate(TENANT_ID, new PessoaVinculoRequest(
      "Pessoa Concorrente",
      "",
      "CPF",
      documento)).getId();
  }
}
