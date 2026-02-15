package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.TipoEntidade;
import com.ia.app.domain.TipoEntidadeConfigPorAgrupador;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.TipoEntidadeConfigPorAgrupadorRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(AuditingConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TipoEntidadeConfigPorAgrupadorConcurrencyTest {

  private static final long TENANT_ID = 19L;

  @Autowired
  private TipoEntidadeRepository tipoEntidadeRepository;

  @Autowired
  private AgrupadorEmpresaRepository agrupadorEmpresaRepository;

  @Autowired
  private TipoEntidadeConfigPorAgrupadorRepository configRepository;

  @Test
  void shouldAllowOnlyOneActiveConfigForSameTipoAndAgrupadorUnderConcurrency() throws Exception {
    TipoEntidade tipo = new TipoEntidade();
    tipo.setTenantId(TENANT_ID);
    tipo.setNome("LOGISTICA");
    tipo.setAtivo(true);
    tipo.setTipoPadrao(false);
    TipoEntidade savedTipo = tipoEntidadeRepository.save(tipo);

    AgrupadorEmpresa agrupador = new AgrupadorEmpresa();
    agrupador.setTenantId(TENANT_ID);
    agrupador.setConfigType(ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE);
    agrupador.setConfigId(savedTipo.getId());
    agrupador.setNome("Grupo Concorrente");
    agrupador.setAtivo(true);
    AgrupadorEmpresa savedAgrupador = agrupadorEmpresaRepository.save(agrupador);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    Callable<String> c1 = () -> salvarConfig(ready, start, savedTipo.getId(), savedAgrupador.getId());
    Callable<String> c2 = () -> salvarConfig(ready, start, savedTipo.getId(), savedAgrupador.getId());

    Future<String> f1 = pool.submit(c1);
    Future<String> f2 = pool.submit(c2);

    assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
    start.countDown();

    List<String> resultados = new ArrayList<>();
    resultados.add(f1.get(5, TimeUnit.SECONDS));
    resultados.add(f2.get(5, TimeUnit.SECONDS));
    pool.shutdownNow();

    long sucesso = resultados.stream().filter("OK"::equals).count();
    long conflito = resultados.stream().filter("CONFLICT"::equals).count();
    assertThat(sucesso).isEqualTo(1);
    assertThat(conflito).isEqualTo(1);

    assertThat(configRepository.findAllByTenantIdAndTipoEntidadeIdAndAtivoTrue(TENANT_ID, savedTipo.getId()))
      .hasSize(1);
  }

  private String salvarConfig(CountDownLatch ready, CountDownLatch start, Long tipoEntidadeId, Long agrupadorId) {
    try {
      ready.countDown();
      start.await(2, TimeUnit.SECONDS);
      TipoEntidadeConfigPorAgrupador cfg = new TipoEntidadeConfigPorAgrupador();
      cfg.setTenantId(TENANT_ID);
      cfg.setTipoEntidadeId(tipoEntidadeId);
      cfg.setAgrupadorId(agrupadorId);
      cfg.setObrigarUmTelefone(true);
      cfg.setAtivo(true);
      configRepository.save(cfg);
      return "OK";
    } catch (DataIntegrityViolationException ex) {
      return "CONFLICT";
    } catch (Exception ex) {
      return "ERROR";
    }
  }
}
