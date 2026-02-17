package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.Empresa;
import com.ia.app.domain.MovimentoConfig;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.domain.TipoEntidade;
import com.ia.app.dto.MovimentoConfigRequest;
import com.ia.app.dto.MovimentoConfigResolverResponse;
import com.ia.app.dto.MovimentoConfigResponse;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.MovimentoConfigRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
  AuditingConfig.class,
  AuditService.class,
  MovimentoConfigService.class
})
class MovimentoConfigResolverServiceTest {

  @Autowired
  private MovimentoConfigService service;

  @Autowired
  private MovimentoConfigRepository movimentoConfigRepository;

  @Autowired
  private EmpresaRepository empresaRepository;

  @Autowired
  private TipoEntidadeRepository tipoEntidadeRepository;

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void shouldResolveUniqueFallbackConfiguration() {
    TenantContext.setTenantId(511L);
    Empresa empresa = createEmpresa(511L, "MATRIZ", null, "Matriz 511", "00511000000001");
    TipoEntidade tipo = createTipoEntidade(511L, "Cliente");

    MovimentoConfigResponse fallback = service.create(new MovimentoConfigRequest(
      MovimentoTipo.ORDEM_COMPRA,
      "OC fallback",
      null,
      true,
      List.of(empresa.getId()),
      List.of(tipo.getId()),
      tipo.getId()));

    MovimentoConfigResolverResponse resolved = service.resolve(
      MovimentoTipo.ORDEM_COMPRA,
      empresa.getId(),
      null);

    assertThat(resolved.configuracaoId()).isEqualTo(fallback.id());
  }

  @Test
  void shouldPreferSpecificContextOverFallback() {
    TenantContext.setTenantId(512L);
    Empresa empresa = createEmpresa(512L, "MATRIZ", null, "Matriz 512", "00512000000001");
    TipoEntidade tipo = createTipoEntidade(512L, "Cliente");

    MovimentoConfigResponse fallback = service.create(new MovimentoConfigRequest(
      MovimentoTipo.PEDIDO_VENDA,
      "PV fallback",
      null,
      true,
      List.of(empresa.getId()),
      List.of(tipo.getId()),
      tipo.getId()));

    MovimentoConfigResponse specific = service.create(new MovimentoConfigRequest(
      MovimentoTipo.PEDIDO_VENDA,
      "PV revenda",
      "REVENDA",
      true,
      List.of(empresa.getId()),
      List.of(tipo.getId()),
      tipo.getId()));

    MovimentoConfigResolverResponse resolvedSpecific = service.resolve(
      MovimentoTipo.PEDIDO_VENDA,
      empresa.getId(),
      "revenda");

    MovimentoConfigResolverResponse resolvedFallback = service.resolve(
      MovimentoTipo.PEDIDO_VENDA,
      empresa.getId(),
      null);

    assertThat(resolvedSpecific.configuracaoId()).isEqualTo(specific.id());
    assertThat(resolvedFallback.configuracaoId()).isEqualTo(fallback.id());
  }

  @Test
  void shouldReturnErrorWhenNoApplicableConfiguration() {
    TenantContext.setTenantId(513L);
    Empresa empresa = createEmpresa(513L, "MATRIZ", null, "Matriz 513", "00513000000001");

    assertThatThrownBy(() -> service.resolve(MovimentoTipo.COTACAO_COMPRA, empresa.getId(), null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("movimento_config_nao_encontrada");
  }

  @Test
  void shouldReturnErrorWhenResolverFindsAmbiguousTie() {
    TenantContext.setTenantId(514L);
    Empresa empresa = createEmpresa(514L, "MATRIZ", null, "Matriz 514", "00514000000001");
    TipoEntidade tipo = createTipoEntidade(514L, "Cliente");

    createRawConfig(514L, "OC empatada A", empresa.getId(), tipo.getId());
    createRawConfig(514L, "OC empatada B", empresa.getId(), tipo.getId());

    assertThatThrownBy(() -> service.resolve(MovimentoTipo.ORDEM_COMPRA, empresa.getId(), null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("movimento_config_conflito_resolucao");
  }

  private void createRawConfig(Long tenantId, String nome, Long empresaId, Long tipoEntidadeId) {
    MovimentoConfig entity = new MovimentoConfig();
    entity.setTenantId(tenantId);
    entity.setTipoMovimento(MovimentoTipo.ORDEM_COMPRA);
    entity.setNome(nome);
    entity.setDescricao(null);
    entity.setPrioridade(100);
    entity.setContextoKey(null);
    entity.setTipoEntidadePadraoId(tipoEntidadeId);
    entity.setAtivo(true);
    entity.replaceEmpresas(List.of(empresaId));
    entity.replaceTiposEntidadePermitidos(List.of(tipoEntidadeId));
    movimentoConfigRepository.saveAndFlush(entity);
  }

  private Empresa createEmpresa(
      Long tenantId,
      String tipo,
      Empresa matriz,
      String razaoSocial,
      String cnpj) {
    Empresa empresa = new Empresa();
    empresa.setTenantId(tenantId);
    empresa.setTipo(tipo);
    empresa.setMatriz(matriz);
    empresa.setRazaoSocial(razaoSocial);
    empresa.setNomeFantasia(razaoSocial);
    empresa.setCnpj(cnpj);
    empresa.setAtivo(true);
    return empresaRepository.save(empresa);
  }

  private TipoEntidade createTipoEntidade(Long tenantId, String nome) {
    TipoEntidade tipo = new TipoEntidade();
    tipo.setTenantId(tenantId);
    tipo.setNome(nome);
    tipo.setCodigoSeed(null);
    tipo.setTipoPadrao(false);
    tipo.setAtivo(true);
    return tipoEntidadeRepository.save(tipo);
  }
}
