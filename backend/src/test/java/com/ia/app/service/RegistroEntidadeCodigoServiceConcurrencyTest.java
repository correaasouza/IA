package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.TipoEntidade;
import com.ia.app.domain.TipoEntidadeConfigPorAgrupador;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.RegistroEntidadeCodigoSeqRepository;
import com.ia.app.repository.TipoEntidadeConfigPorAgrupadorRepository;
import com.ia.app.repository.TipoEntidadeRepository;
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
@Import({AuditingConfig.class, RegistroEntidadeCodigoService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RegistroEntidadeCodigoServiceConcurrencyTest {

  private static final long TENANT_ID = 32L;

  @Autowired
  private RegistroEntidadeCodigoService service;

  @Autowired
  private TipoEntidadeRepository tipoEntidadeRepository;

  @Autowired
  private AgrupadorEmpresaRepository agrupadorEmpresaRepository;

  @Autowired
  private TipoEntidadeConfigPorAgrupadorRepository configRepository;

  @Autowired
  private RegistroEntidadeCodigoSeqRepository seqRepository;

  @Test
  void shouldGenerateDistinctCodesWhenConcurrentRequestsUseSameConfig() throws Exception {
    Long configId = createTipoEntidadeConfigAgrupador();

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    Callable<Long> c1 = () -> nextCodeWithSync(ready, start, configId);
    Callable<Long> c2 = () -> nextCodeWithSync(ready, start, configId);

    Future<Long> f1 = pool.submit(c1);
    Future<Long> f2 = pool.submit(c2);

    assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
    start.countDown();

    List<Long> generated = new ArrayList<>();
    generated.add(f1.get(5, TimeUnit.SECONDS));
    generated.add(f2.get(5, TimeUnit.SECONDS));
    pool.shutdownNow();

    Set<Long> distinct = new HashSet<>(generated);
    assertThat(distinct).hasSize(2);
    assertThat(distinct).containsExactlyInAnyOrder(1L, 2L);

    var seq = seqRepository.findByTenantIdAndTipoEntidadeConfigAgrupadorId(TENANT_ID, configId).orElseThrow();
    assertThat(seq.getNextValue()).isEqualTo(3L);
  }

  @Test
  void shouldRestartSequenceForDifferentConfigurations() {
    Long configA = createTipoEntidadeConfigAgrupador();
    Long configB = createTipoEntidadeConfigAgrupador();

    Long a1 = service.proximoCodigo(TENANT_ID, configA);
    Long a2 = service.proximoCodigo(TENANT_ID, configA);
    Long b1 = service.proximoCodigo(TENANT_ID, configB);

    assertThat(a1).isEqualTo(1L);
    assertThat(a2).isEqualTo(2L);
    assertThat(b1).isEqualTo(1L);
  }

  private Long nextCodeWithSync(CountDownLatch ready, CountDownLatch start, Long configId) throws Exception {
    ready.countDown();
    start.await(2, TimeUnit.SECONDS);
    return service.proximoCodigo(TENANT_ID, configId);
  }

  private Long createTipoEntidadeConfigAgrupador() {
    TipoEntidade tipo = new TipoEntidade();
    tipo.setTenantId(TENANT_ID);
    tipo.setNome("TIPO-CFG-" + System.nanoTime());
    tipo.setTipoPadrao(false);
    tipo.setAtivo(true);
    TipoEntidade savedTipo = tipoEntidadeRepository.save(tipo);

    AgrupadorEmpresa agrupador = new AgrupadorEmpresa();
    agrupador.setTenantId(TENANT_ID);
    agrupador.setConfigType(ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE);
    agrupador.setConfigId(savedTipo.getId());
    agrupador.setNome("AGRUPADOR-" + System.nanoTime());
    agrupador.setAtivo(true);
    AgrupadorEmpresa savedAgrupador = agrupadorEmpresaRepository.save(agrupador);

    TipoEntidadeConfigPorAgrupador cfg = new TipoEntidadeConfigPorAgrupador();
    cfg.setTenantId(TENANT_ID);
    cfg.setTipoEntidadeId(savedTipo.getId());
    cfg.setAgrupadorId(savedAgrupador.getId());
    cfg.setObrigarUmTelefone(false);
    cfg.setAtivo(true);
    return configRepository.save(cfg).getId();
  }
}
