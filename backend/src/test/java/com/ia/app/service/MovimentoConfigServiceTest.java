package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.Empresa;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.domain.TipoEntidade;
import com.ia.app.dto.MovimentoConfigRequest;
import com.ia.app.dto.MovimentoConfigCoverageWarningResponse;
import com.ia.app.dto.MovimentoConfigResponse;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
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
class MovimentoConfigServiceTest {

  @Autowired
  private MovimentoConfigService service;

  @Autowired
  private EmpresaRepository empresaRepository;

  @Autowired
  private TipoEntidadeRepository tipoEntidadeRepository;

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void shouldCreateValidConfiguration() {
    TenantContext.setTenantId(501L);
    Empresa empresaA = createEmpresa(501L, "MATRIZ", null, "Matriz 501", "00501000000001");
    Empresa empresaB = createEmpresa(501L, "FILIAL", empresaA, "Filial 501", "00501000000002");
    TipoEntidade tipoA = createTipoEntidade(501L, "Cliente");
    TipoEntidade tipoB = createTipoEntidade(501L, "Fornecedor");

    MovimentoConfigRequest request = new MovimentoConfigRequest(
      MovimentoTipo.ORDEM_COMPRA,
      "OC principal",
      null,
      true,
      List.of(empresaA.getId(), empresaB.getId()),
      List.of(tipoA.getId(), tipoB.getId()),
      tipoA.getId());

    MovimentoConfigResponse response = service.create(request);

    assertThat(response.id()).isNotNull();
    assertThat(response.tipoMovimento()).isEqualTo(MovimentoTipo.ORDEM_COMPRA);
    assertThat(response.empresaIds()).containsExactly(empresaA.getId(), empresaB.getId());
    assertThat(response.tiposEntidadePermitidos()).containsExactly(tipoA.getId(), tipoB.getId());
    assertThat(response.tipoEntidadePadraoId()).isEqualTo(tipoA.getId());
  }

  @Test
  void shouldAllowConfigurationWithoutDestinatario() {
    TenantContext.setTenantId(508L);
    Empresa empresaA = createEmpresa(508L, "MATRIZ", null, "Matriz 508", "00508000000001");

    MovimentoConfigRequest request = new MovimentoConfigRequest(
      MovimentoTipo.PEDIDO_EQUIPAMENTO,
      "PE sem destinatario",
      null,
      true,
      List.of(empresaA.getId()),
      List.of(),
      null);

    MovimentoConfigResponse response = service.create(request);

    assertThat(response.id()).isNotNull();
    assertThat(response.tiposEntidadePermitidos()).isEmpty();
    assertThat(response.tipoEntidadePadraoId()).isNull();
  }

  @Test
  void shouldBlockDefaultOutsideAllowed() {
    TenantContext.setTenantId(502L);
    Empresa empresa = createEmpresa(502L, "MATRIZ", null, "Matriz 502", "00502000000001");
    TipoEntidade tipoA = createTipoEntidade(502L, "Cliente");
    TipoEntidade tipoB = createTipoEntidade(502L, "Fornecedor");

    MovimentoConfigRequest request = new MovimentoConfigRequest(
      MovimentoTipo.PEDIDO_VENDA,
      "PV padrao",
      null,
      true,
      List.of(empresa.getId()),
      List.of(tipoA.getId()),
      tipoB.getId());

    assertThatThrownBy(() -> service.create(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("movimento_config_tipo_padrao_fora_permitidos");
  }

  @Test
  void shouldBlockConflictByPriorityContextAndCompany() {
    TenantContext.setTenantId(503L);
    Empresa empresa = createEmpresa(503L, "MATRIZ", null, "Matriz 503", "00503000000001");
    TipoEntidade tipo = createTipoEntidade(503L, "Cliente");

    MovimentoConfigRequest first = new MovimentoConfigRequest(
      MovimentoTipo.PEDIDO_VENDA,
      "PV principal",
      null,
      true,
      List.of(empresa.getId()),
      List.of(tipo.getId()),
      tipo.getId());
    service.create(first);

    MovimentoConfigRequest second = new MovimentoConfigRequest(
      MovimentoTipo.PEDIDO_VENDA,
      "PV secundario",
      null,
      true,
      List.of(empresa.getId()),
      List.of(tipo.getId()),
      tipo.getId());

    assertThatThrownBy(() -> service.create(second))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("movimento_config_conflito_prioridade_contexto_empresa");
  }

  @Test
  void shouldRespectTenantIsolation() {
    TenantContext.setTenantId(504L);
    Empresa empresa = createEmpresa(504L, "MATRIZ", null, "Matriz 504", "00504000000001");
    TipoEntidade tipo = createTipoEntidade(504L, "Cliente");
    MovimentoConfigResponse created = service.create(new MovimentoConfigRequest(
      MovimentoTipo.ORCAMENTO_VENDA,
      "OV tenant 504",
      null,
      true,
      List.of(empresa.getId()),
      List.of(tipo.getId()),
      tipo.getId()));

    TenantContext.setTenantId(505L);
    assertThatThrownBy(() -> service.getById(created.id()))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("movimento_config_not_found");
  }

  @Test
  void shouldListCoverageWarningsWhenCombinationIsMissing() {
    TenantContext.setTenantId(506L);
    Empresa empresaA = createEmpresa(506L, "MATRIZ", null, "Matriz 506", "00506000000001");
    createEmpresa(506L, "FILIAL", empresaA, "Filial 506", "00506000000002");
    TipoEntidade tipo = createTipoEntidade(506L, "Cliente");

    service.create(new MovimentoConfigRequest(
      MovimentoTipo.ORDEM_COMPRA,
      "OC base matriz",
      null,
      true,
      List.of(empresaA.getId()),
      List.of(tipo.getId()),
      tipo.getId()));

    List<MovimentoConfigCoverageWarningResponse> warnings = service.listCoverageWarnings();

    assertThat(warnings).isNotEmpty();
    assertThat(warnings).anyMatch(item ->
      item.empresaId() != null
        && item.tipoMovimento() == MovimentoTipo.ORDEM_COMPRA);
  }

  @Test
  void shouldListMenuTypesForEmpresaWithActiveConfigs() {
    TenantContext.setTenantId(507L);
    Empresa empresaA = createEmpresa(507L, "MATRIZ", null, "Matriz 507", "00507000000001");
    Empresa empresaB = createEmpresa(507L, "FILIAL", empresaA, "Filial 507", "00507000000002");
    TipoEntidade tipo = createTipoEntidade(507L, "Cliente");

    service.create(new MovimentoConfigRequest(
      MovimentoTipo.ORDEM_COMPRA,
      "OC matriz",
      null,
      true,
      List.of(empresaA.getId()),
      List.of(tipo.getId()),
      tipo.getId()));
    service.create(new MovimentoConfigRequest(
      MovimentoTipo.PEDIDO_VENDA,
      "PV filial",
      null,
      true,
      List.of(empresaB.getId()),
      List.of(tipo.getId()),
      tipo.getId()));

    List<com.ia.app.dto.MovimentoTipoResponse> menuA = service.listMenuTiposForEmpresa(empresaA.getId());
    List<com.ia.app.dto.MovimentoTipoResponse> menuB = service.listMenuTiposForEmpresa(empresaB.getId());

    assertThat(menuA).extracting(com.ia.app.dto.MovimentoTipoResponse::codigo).containsExactly("ORDEM_COMPRA");
    assertThat(menuB).extracting(com.ia.app.dto.MovimentoTipoResponse::codigo).containsExactly("PEDIDO_VENDA");
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
