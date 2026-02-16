package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.AgrupadorEmpresaItem;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogNumberingMode;
import com.ia.app.domain.Empresa;
import com.ia.app.dto.CatalogGroupRequest;
import com.ia.app.dto.CatalogItemRequest;
import com.ia.app.dto.CatalogItemResponse;
import com.ia.app.repository.AgrupadorEmpresaItemRepository;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.tenant.EmpresaContext;
import com.ia.app.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({
  AuditingConfig.class,
  CatalogConfigurationService.class,
  CatalogConfigurationByGroupService.class,
  CatalogConfigurationGroupSyncService.class,
  CatalogStockTypeSyncService.class,
  CatalogItemContextService.class,
  CatalogStockQueryService.class,
  CatalogItemCodeService.class,
  CatalogItemCrudSupportService.class,
  CatalogProductService.class,
  CatalogServiceCrudService.class,
  CatalogGroupService.class,
  AuditService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CatalogCrudServiceTest {

  @Autowired
  private CatalogConfigurationService configurationService;

  @Autowired
  private CatalogConfigurationByGroupService configurationByGroupService;

  @Autowired
  private CatalogItemContextService contextService;

  @Autowired
  private CatalogProductService productService;

  @Autowired
  private CatalogGroupService groupService;

  @Autowired
  private CatalogStockQueryService stockQueryService;

  @Autowired
  private AgrupadorEmpresaRepository agrupadorRepository;

  @Autowired
  private AgrupadorEmpresaItemRepository agrupadorItemRepository;

  @Autowired
  private EmpresaRepository empresaRepository;

  @AfterEach
  void cleanContext() {
    EmpresaContext.clear();
    TenantContext.clear();
  }

  @Test
  void shouldReturnContextNotLinkedWhenEmpresaHasNoGroup() {
    Long tenantId = 101L;
    Long empresaId = createEmpresa(tenantId, "10100000000001");
    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaId);

    var context = contextService.contexto(CatalogConfigurationType.PRODUCTS);

    assertThat(context.vinculado()).isFalse();
    assertThat(context.motivo()).isEqualTo("EMPRESA_SEM_GRUPO_NO_CATALOGO");
  }

  @Test
  void shouldGenerateAutomaticCodesWithoutCollisionOnConcurrentCreates() throws Exception {
    Long tenantId = 102L;
    Long empresaId = createEmpresa(tenantId, "10200000000001");
    setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    Callable<Long> c1 = () -> createProductWithSync(tenantId, empresaId, ready, start, "Item concorrente A");
    Callable<Long> c2 = () -> createProductWithSync(tenantId, empresaId, ready, start, "Item concorrente B");
    Future<Long> f1 = pool.submit(c1);
    Future<Long> f2 = pool.submit(c2);

    assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
    start.countDown();

    List<Long> codes = new ArrayList<>();
    codes.add(f1.get(5, TimeUnit.SECONDS));
    codes.add(f2.get(5, TimeUnit.SECONDS));
    pool.shutdownNow();

    assertThat(codes).containsExactlyInAnyOrder(1L, 2L);
  }

  @Test
  void shouldRejectDuplicateManualCodeInsideSameScope() {
    Long tenantId = 103L;
    Long empresaId = createEmpresa(tenantId, "10300000000001");
    var scope = setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");

    TenantContext.setTenantId(tenantId);
    configurationByGroupService.update(CatalogConfigurationType.PRODUCTS, scope.agrupadorId(), CatalogNumberingMode.MANUAL);
    EmpresaContext.setEmpresaId(empresaId);

    CatalogItemRequest payload = new CatalogItemRequest(77L, "Item manual", "desc", null, true);
    productService.create(payload);

    assertThatThrownBy(() -> productService.create(new CatalogItemRequest(77L, "Item manual 2", null, null, true)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("catalog_item_codigo_duplicado");
  }

  @Test
  void shouldFilterByTextAndGroup() {
    Long tenantId = 104L;
    Long empresaId = createEmpresa(tenantId, "10400000000001");
    var scope = setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");
    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaId);

    var group = groupService.create(CatalogConfigurationType.PRODUCTS, new CatalogGroupRequest("Lubrificantes", null));
    productService.create(new CatalogItemRequest(null, "OLEO 5W30", "API SN", group.getId(), true));
    productService.create(new CatalogItemRequest(null, "FILTRO AR", "Papel", null, true));

    var byText = productService.list(null, "oleo", null, true, PageRequest.of(0, 20));
    var byGroup = productService.list(null, null, group.getId(), true, PageRequest.of(0, 20));

    assertThat(byText.getTotalElements()).isEqualTo(1);
    assertThat(byText.getContent().get(0).nome()).isEqualTo("OLEO 5W30");
    assertThat(byGroup.getTotalElements()).isEqualTo(1);
    assertThat(byGroup.getContent().get(0).catalogGroupId()).isEqualTo(group.getId());
  }

  @Test
  void shouldBlockDeleteGroupWhenItemsExistInSubtree() {
    Long tenantId = 105L;
    Long empresaId = createEmpresa(tenantId, "10500000000001");
    setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");
    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaId);

    var root = groupService.create(CatalogConfigurationType.PRODUCTS, new CatalogGroupRequest("Raiz", null));
    var child = groupService.create(CatalogConfigurationType.PRODUCTS, new CatalogGroupRequest("Filho", root.getId()));
    productService.create(new CatalogItemRequest(null, "ITEM VINCULADO", null, child.getId(), true));

    assertThatThrownBy(() -> groupService.delete(CatalogConfigurationType.PRODUCTS, root.getId()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("catalog_group_possui_itens");
  }

  @Test
  void shouldAllowGetAndReactivateInactiveItemInSameScope() {
    Long tenantId = 106L;
    Long empresaId = createEmpresa(tenantId, "10600000000001");
    setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");
    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaId);

    CatalogItemResponse created = productService.create(new CatalogItemRequest(null, "ITEM INATIVO", null, null, true));

    CatalogItemResponse inactive = productService.update(
      created.id(),
      new CatalogItemRequest(null, created.nome(), created.descricao(), created.catalogGroupId(), false));
    assertThat(inactive.ativo()).isFalse();

    CatalogItemResponse loadedInactive = productService.get(created.id());
    assertThat(loadedInactive.ativo()).isFalse();

    CatalogItemResponse reactivated = productService.update(
      created.id(),
      new CatalogItemRequest(null, created.nome(), created.descricao(), created.catalogGroupId(), true));
    assertThat(reactivated.ativo()).isTrue();
  }

  @Test
  void shouldReturnEmptyLedgerWithoutError() {
    Long tenantId = 107L;
    Long empresaId = createEmpresa(tenantId, "10700000000001");
    setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");
    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaId);

    CatalogItemResponse created = productService.create(new CatalogItemRequest(null, "ITEM SEM MOV", null, null, true));

    var page = stockQueryService.loadLedger(
      CatalogConfigurationType.PRODUCTS,
      created.id(),
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      PageRequest.of(0, 10));

    assertThat(page.getTotalElements()).isZero();
    assertThat(page.getContent()).isEmpty();
  }

  private Long createProductWithSync(
      Long tenantId,
      Long empresaId,
      CountDownLatch ready,
      CountDownLatch start,
      String nome) throws Exception {
    try {
      TenantContext.setTenantId(tenantId);
      EmpresaContext.setEmpresaId(empresaId);
      ready.countDown();
      start.await(2, TimeUnit.SECONDS);
      CatalogItemResponse created = productService.create(new CatalogItemRequest(null, nome, null, null, true));
      return created.codigo();
    } finally {
      EmpresaContext.clear();
      TenantContext.clear();
    }
  }

  private CatalogItemContextService.CatalogItemScope setupCatalogGroupLink(
      Long tenantId,
      Long empresaId,
      CatalogConfigurationType type,
      String nomeAgrupador) {
    TenantContext.setTenantId(tenantId);
    Long configId = configurationService.getOrCreate(type).id();
    AgrupadorEmpresa group = new AgrupadorEmpresa();
    group.setTenantId(tenantId);
    group.setConfigType(ConfiguracaoScopeService.TYPE_CATALOGO);
    group.setConfigId(configId);
    group.setNome(nomeAgrupador);
    group.setAtivo(true);
    group = agrupadorRepository.save(group);

    AgrupadorEmpresaItem item = new AgrupadorEmpresaItem();
    item.setTenantId(tenantId);
    item.setConfigType(ConfiguracaoScopeService.TYPE_CATALOGO);
    item.setConfigId(configId);
    item.setAgrupador(group);
    item.setEmpresaId(empresaId);
    agrupadorItemRepository.save(item);

    EmpresaContext.setEmpresaId(empresaId);
    return contextService.resolveObrigatorio(type);
  }

  private Long createEmpresa(Long tenantId, String cnpj) {
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
