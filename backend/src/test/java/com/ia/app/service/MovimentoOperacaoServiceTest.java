package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.Empresa;
import com.ia.app.domain.MovimentoConfig;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.dto.MovimentoEstoqueCreateRequest;
import com.ia.app.dto.MovimentoEstoqueResponse;
import com.ia.app.dto.MovimentoEstoqueTemplateResponse;
import com.ia.app.dto.MovimentoTemplateRequest;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.MovimentoConfigRepository;
import com.ia.app.tenant.EmpresaContext;
import com.ia.app.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import({
  AuditingConfig.class,
  AuditService.class,
  MovimentoConfigService.class,
  MovimentoEstoqueOperacaoHandler.class,
  MovimentoOperacaoService.class,
  MovimentoOperacaoServiceTest.JacksonConfig.class
})
class MovimentoOperacaoServiceTest {

  @Autowired
  private EmpresaRepository empresaRepository;

  @Autowired
  private MovimentoConfigRepository movimentoConfigRepository;

  @Autowired
  private MovimentoEstoqueOperacaoHandler estoqueHandler;

  @Autowired
  private MovimentoOperacaoService operacaoService;

  @Autowired
  private ObjectMapper objectMapper;

  @AfterEach
  void clearContext() {
    EmpresaContext.clear();
    TenantContext.clear();
  }

  @Test
  void shouldBuildTemplateCreateAndListMovimentoEstoque() {
    Long tenantId = 801L;
    TenantContext.setTenantId(tenantId);
    Empresa empresa = createEmpresa(tenantId, "00801000000001");
    EmpresaContext.setEmpresaId(empresa.getId());
    MovimentoConfig config = createMovimentoConfigEstoque(tenantId, empresa.getId());

    MovimentoEstoqueTemplateResponse template = estoqueHandler.buildTemplate(
      new MovimentoTemplateRequest(empresa.getId()));

    assertThat(template.tipoMovimento()).isEqualTo(MovimentoTipo.MOVIMENTO_ESTOQUE);
    assertThat(template.empresaId()).isEqualTo(empresa.getId());
    assertThat(template.movimentoConfigId()).isEqualTo(config.getId());

    MovimentoEstoqueCreateRequest request = new MovimentoEstoqueCreateRequest(
      empresa.getId(),
      "Movimento de ajuste 31/12/2025",
      null);
    MovimentoEstoqueResponse created = estoqueHandler.create(objectMapper.valueToTree(request));

    assertThat(created.id()).isNotNull();
    assertThat(created.empresaId()).isEqualTo(empresa.getId());
    assertThat(created.nome()).isEqualTo("Movimento de ajuste 31/12/2025");
    assertThat(created.movimentoConfigId()).isEqualTo(config.getId());

    Page<MovimentoEstoqueResponse> page = estoqueHandler.list(PageRequest.of(0, 20), null, null, null);
    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent().get(0).id()).isEqualTo(created.id());
  }

  @Test
  void shouldRejectPayloadEmpresaDifferentFromContextEmpresa() {
    Long tenantId = 802L;
    TenantContext.setTenantId(tenantId);
    Empresa empresaContext = createEmpresa(tenantId, "00802000000001");
    Empresa outraEmpresa = createEmpresa(tenantId, "00802000000002");
    EmpresaContext.setEmpresaId(empresaContext.getId());
    createMovimentoConfigEstoque(tenantId, empresaContext.getId());

    MovimentoEstoqueCreateRequest request = new MovimentoEstoqueCreateRequest(
      outraEmpresa.getId(),
      "Movimento fora do contexto",
      null);

    assertThatThrownBy(() -> estoqueHandler.create(objectMapper.valueToTree(request)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("movimento_empresa_context_mismatch");
  }

  @Test
  void shouldFailTemplateWhenNoConfigApplies() {
    Long tenantId = 803L;
    TenantContext.setTenantId(tenantId);
    Empresa empresa = createEmpresa(tenantId, "00803000000001");
    EmpresaContext.setEmpresaId(empresa.getId());

    assertThatThrownBy(() -> estoqueHandler.buildTemplate(new MovimentoTemplateRequest(empresa.getId())))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("movimento_config_nao_encontrada");
  }

  @Test
  void shouldRejectMovimentoTipoNotImplementedInFacade() {
    Long tenantId = 804L;
    TenantContext.setTenantId(tenantId);
    Empresa empresa = createEmpresa(tenantId, "00804000000001");
    EmpresaContext.setEmpresaId(empresa.getId());

    assertThatThrownBy(() -> operacaoService.create(MovimentoTipo.PEDIDO_VENDA, objectMapper.createObjectNode()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("movimento_tipo_nao_implementado");
  }

  private Empresa createEmpresa(Long tenantId, String cnpj) {
    Empresa empresa = new Empresa();
    empresa.setTenantId(tenantId);
    empresa.setTipo(Empresa.TIPO_FILIAL);
    empresa.setRazaoSocial("Filial " + cnpj);
    empresa.setNomeFantasia("Filial " + cnpj);
    empresa.setCnpj(cnpj);
    empresa.setAtivo(true);
    return empresaRepository.saveAndFlush(empresa);
  }

  private MovimentoConfig createMovimentoConfigEstoque(Long tenantId, Long empresaId) {
    MovimentoConfig config = new MovimentoConfig();
    config.setTenantId(tenantId);
    config.setTipoMovimento(MovimentoTipo.MOVIMENTO_ESTOQUE);
    config.setNome("Config Estoque " + empresaId);
    config.setDescricao(null);
    config.setPrioridade(100);
    config.setContextoKey(null);
    config.setTipoEntidadePadraoId(null);
    config.setAtivo(true);
    config.replaceEmpresas(java.util.List.of(empresaId));
    config.replaceTiposEntidadePermitidos(java.util.List.of());
    return movimentoConfigRepository.saveAndFlush(config);
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
