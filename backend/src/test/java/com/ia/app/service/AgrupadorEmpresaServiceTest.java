package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.ConfigFormulario;
import com.ia.app.domain.Empresa;
import com.ia.app.dto.AgrupadorEmpresaResponse;
import com.ia.app.repository.ConfigFormularioRepository;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({
  AuditingConfig.class,
  AgrupadorEmpresaService.class,
  ConfiguracaoScopeService.class,
  TipoEntidadeConfigAgrupadorSyncService.class,
  AuditService.class,
  AgrupadorEmpresaServiceTest.TestMetricsConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AgrupadorEmpresaServiceTest {

  @TestConfiguration
  static class TestMetricsConfig {
    @Bean
    MeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }
  }

  private static final long TENANT_ID = 7L;

  @Autowired
  private AgrupadorEmpresaService service;

  @Autowired
  private ConfigFormularioRepository configFormularioRepository;

  @Autowired
  private EmpresaRepository empresaRepository;

  @AfterEach
  void cleanTenantContext() {
    TenantContext.clear();
  }

  @Test
  void shouldRejectSameEmpresaInDifferentGroupsForSameConfig() {
    Long configId = criarConfigFormulario(TENANT_ID);
    Long empresaId = criarEmpresaMatriz(TENANT_ID, "12345678000111");

    TenantContext.setTenantId(TENANT_ID);
    AgrupadorEmpresaResponse g1 = service.criar("FORMULARIO", configId, "Grupo A");
    AgrupadorEmpresaResponse g2 = service.criar("FORMULARIO", configId, "Grupo B");

    service.adicionarEmpresa("FORMULARIO", configId, g1.id(), empresaId);

    assertThatThrownBy(() -> service.adicionarEmpresa("FORMULARIO", configId, g2.id(), empresaId))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("empresa_ja_vinculada_outro_agrupador");
  }

  @Test
  void shouldAllowOnlyOneConcurrentLinkForSameEmpresaAndConfig() throws Exception {
    Long configId = criarConfigFormulario(TENANT_ID);
    Long empresaId = criarEmpresaMatriz(TENANT_ID, "12345678000122");

    TenantContext.setTenantId(TENANT_ID);
    AgrupadorEmpresaResponse g1 = service.criar("FORMULARIO", configId, "Grupo 1");
    AgrupadorEmpresaResponse g2 = service.criar("FORMULARIO", configId, "Grupo 2");
    TenantContext.clear();

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    Callable<String> c1 = () -> vincularComSincronizacao(ready, start, configId, g1.id(), empresaId);
    Callable<String> c2 = () -> vincularComSincronizacao(ready, start, configId, g2.id(), empresaId);

    Future<String> f1 = pool.submit(c1);
    Future<String> f2 = pool.submit(c2);

    assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
    start.countDown();

    List<String> results = new ArrayList<>();
    results.add(f1.get(5, TimeUnit.SECONDS));
    results.add(f2.get(5, TimeUnit.SECONDS));
    pool.shutdownNow();

    long success = results.stream().filter("OK"::equals).count();
    long conflicts = results.stream().filter("CONFLICT"::equals).count();
    assertThat(success).isEqualTo(1);
    assertThat(conflicts).isEqualTo(1);

    TenantContext.setTenantId(TENANT_ID);
    List<AgrupadorEmpresaResponse> grupos = service.listar("FORMULARIO", configId);
    long gruposComEmpresa = grupos.stream().filter(g -> g.empresas().stream().anyMatch(e -> e.empresaId().equals(empresaId))).count();
    assertThat(gruposComEmpresa).isEqualTo(1);
  }

  private String vincularComSincronizacao(
      CountDownLatch ready,
      CountDownLatch start,
      Long configId,
      Long agrupadorId,
      Long empresaId) {
    try {
      TenantContext.setTenantId(TENANT_ID);
      ready.countDown();
      start.await(2, TimeUnit.SECONDS);
      service.adicionarEmpresa("FORMULARIO", configId, agrupadorId, empresaId);
      return "OK";
    } catch (IllegalArgumentException ex) {
      return ex.getMessage() != null && ex.getMessage().contains("empresa_ja_vinculada_outro_agrupador")
        ? "CONFLICT"
        : "ERROR";
    } catch (Exception ex) {
      return "ERROR";
    } finally {
      TenantContext.clear();
    }
  }

  private Long criarConfigFormulario(Long tenantId) {
    ConfigFormulario cfg = new ConfigFormulario();
    cfg.setTenantId(tenantId);
    cfg.setScreenId("entities-test");
    cfg.setScopeTipo("TENANT");
    cfg.setScopeValor(String.valueOf(tenantId));
    cfg.setConfigJson("{}");
    cfg.setVersao(1);
    return configFormularioRepository.save(cfg).getId();
  }

  private Long criarEmpresaMatriz(Long tenantId, String cnpj) {
    Empresa empresa = new Empresa();
    empresa.setTenantId(tenantId);
    empresa.setTipo(Empresa.TIPO_MATRIZ);
    empresa.setRazaoSocial("Empresa " + cnpj);
    empresa.setNomeFantasia("Empresa " + cnpj);
    empresa.setCnpj(cnpj);
    empresa.setAtivo(true);
    return empresaRepository.save(empresa).getId();
  }
}
