package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ia.app.config.AuditingConfig;
import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.AgrupadorEmpresaItem;
import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogMovement;
import com.ia.app.domain.CatalogMovementLine;
import com.ia.app.domain.CatalogMovementMetricType;
import com.ia.app.domain.CatalogMovementOriginType;
import com.ia.app.domain.CatalogNumberingMode;
import com.ia.app.domain.CatalogStockType;
import com.ia.app.domain.Empresa;
import com.ia.app.domain.OfficialUnit;
import com.ia.app.domain.OfficialUnitOrigin;
import com.ia.app.domain.TenantUnit;
import com.ia.app.dto.CatalogGroupRequest;
import com.ia.app.dto.CatalogItemRequest;
import com.ia.app.dto.CatalogItemResponse;
import com.ia.app.repository.AgrupadorEmpresaItemRepository;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.CatalogMovementLineRepository;
import com.ia.app.repository.CatalogMovementRepository;
import com.ia.app.repository.CatalogStockTypeRepository;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.OfficialUnitRepository;
import com.ia.app.repository.TenantUnitRepository;
import com.ia.app.tenant.EmpresaContext;
import com.ia.app.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
  CatalogPriceRuleService.class,
  CatalogItemPriceService.class,
  PriceChangeLogService.class,
  CatalogUnitLockService.class,
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
  private CatalogStockTypeRepository stockTypeRepository;

  @Autowired
  private CatalogMovementRepository movementRepository;

  @Autowired
  private CatalogMovementLineRepository movementLineRepository;

  @Autowired
  private AgrupadorEmpresaRepository agrupadorRepository;

  @Autowired
  private AgrupadorEmpresaItemRepository agrupadorItemRepository;

  @Autowired
  private EmpresaRepository empresaRepository;

  @Autowired
  private OfficialUnitRepository officialUnitRepository;

  @Autowired
  private TenantUnitRepository tenantUnitRepository;

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
    UUID tenantUnitId = createTenantUnit(tenantId);

    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    Callable<Long> c1 = () -> createProductWithSync(tenantId, empresaId, tenantUnitId, ready, start, "Item concorrente A");
    Callable<Long> c2 = () -> createProductWithSync(tenantId, empresaId, tenantUnitId, ready, start, "Item concorrente B");
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
    UUID tenantUnitId = createTenantUnit(tenantId);

    TenantContext.setTenantId(tenantId);
    configurationByGroupService.update(CatalogConfigurationType.PRODUCTS, scope.agrupadorId(), CatalogNumberingMode.MANUAL);
    EmpresaContext.setEmpresaId(empresaId);

    CatalogItemRequest payload = new CatalogItemRequest(77L, "Item manual", "desc", null, tenantUnitId, null, null, true);
    productService.create(payload);

    assertThatThrownBy(() -> productService.create(new CatalogItemRequest(77L, "Item manual 2", null, null, tenantUnitId, null, null, true)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("catalog_item_codigo_duplicado");
  }

  @Test
  void shouldFilterByTextAndGroup() {
    Long tenantId = 104L;
    Long empresaId = createEmpresa(tenantId, "10400000000001");
    var scope = setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");
    UUID tenantUnitId = createTenantUnit(tenantId);
    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaId);

    var group = groupService.create(CatalogConfigurationType.PRODUCTS, new CatalogGroupRequest("Lubrificantes", null));
    productService.create(new CatalogItemRequest(null, "OLEO 5W30", "API SN", group.getId(), tenantUnitId, null, null, true));
    productService.create(new CatalogItemRequest(null, "FILTRO AR", "Papel", null, tenantUnitId, null, null, true));

    var byText = productService.list(null, "oleo", null, false, true, PageRequest.of(0, 20));
    var byGroup = productService.list(null, null, group.getId(), false, true, PageRequest.of(0, 20));

    assertThat(byText.getTotalElements()).isEqualTo(1);
    assertThat(byText.getContent().get(0).nome()).isEqualTo("OLEO 5W30");
    assertThat(byGroup.getTotalElements()).isEqualTo(1);
    assertThat(byGroup.getContent().get(0).catalogGroupId()).isEqualTo(group.getId());
  }

  @Test
  void shouldIncludeDescendantsWhenFilteringByGroup() {
    Long tenantId = 110L;
    Long empresaId = createEmpresa(tenantId, "11000000000001");
    setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");
    UUID tenantUnitId = createTenantUnit(tenantId);
    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaId);

    var root = groupService.create(CatalogConfigurationType.PRODUCTS, new CatalogGroupRequest("Raiz", null));
    var child = groupService.create(CatalogConfigurationType.PRODUCTS, new CatalogGroupRequest("Filho", root.getId()));
    productService.create(new CatalogItemRequest(null, "ITEM FILHO", null, child.getId(), tenantUnitId, null, null, true));

    var withoutDescendants = productService.list(null, null, root.getId(), false, true, PageRequest.of(0, 20));
    var withDescendants = productService.list(null, null, root.getId(), true, true, PageRequest.of(0, 20));

    assertThat(withoutDescendants.getTotalElements()).isEqualTo(0);
    assertThat(withDescendants.getTotalElements()).isEqualTo(1);
    assertThat(withDescendants.getContent().get(0).catalogGroupId()).isEqualTo(child.getId());
  }

  @Test
  void shouldBlockDeleteGroupWhenItemsExistInSubtree() {
    Long tenantId = 105L;
    Long empresaId = createEmpresa(tenantId, "10500000000001");
    setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");
    UUID tenantUnitId = createTenantUnit(tenantId);
    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaId);

    var root = groupService.create(CatalogConfigurationType.PRODUCTS, new CatalogGroupRequest("Raiz", null));
    var child = groupService.create(CatalogConfigurationType.PRODUCTS, new CatalogGroupRequest("Filho", root.getId()));
    productService.create(new CatalogItemRequest(null, "ITEM VINCULADO", null, child.getId(), tenantUnitId, null, null, true));

    assertThatThrownBy(() -> groupService.delete(CatalogConfigurationType.PRODUCTS, root.getId()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("catalog_group_possui_itens");
  }

  @Test
  void shouldAllowGetAndReactivateInactiveItemInSameScope() {
    Long tenantId = 106L;
    Long empresaId = createEmpresa(tenantId, "10600000000001");
    setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");
    UUID tenantUnitId = createTenantUnit(tenantId);
    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaId);

    CatalogItemResponse created = productService.create(new CatalogItemRequest(null, "ITEM INATIVO", null, null, tenantUnitId, null, null, true));

    CatalogItemResponse inactive = productService.update(
      created.id(),
      new CatalogItemRequest(null, created.nome(), created.descricao(), created.catalogGroupId(), created.tenantUnitId(), created.unidadeAlternativaTenantUnitId(), created.fatorConversaoAlternativa(), false));
    assertThat(inactive.ativo()).isFalse();

    CatalogItemResponse loadedInactive = productService.get(created.id());
    assertThat(loadedInactive.ativo()).isFalse();

    CatalogItemResponse reactivated = productService.update(
      created.id(),
      new CatalogItemRequest(null, created.nome(), created.descricao(), created.catalogGroupId(), created.tenantUnitId(), created.unidadeAlternativaTenantUnitId(), created.fatorConversaoAlternativa(), true));
    assertThat(reactivated.ativo()).isTrue();
  }

  @Test
  void shouldReturnEmptyLedgerWithoutError() {
    Long tenantId = 107L;
    Long empresaId = createEmpresa(tenantId, "10700000000001");
    setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");
    UUID tenantUnitId = createTenantUnit(tenantId);
    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaId);

    CatalogItemResponse created = productService.create(new CatalogItemRequest(null, "ITEM SEM MOV", null, null, tenantUnitId, null, null, true));

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
      null,
      null,
      null,
      null,
      PageRequest.of(0, 10));

    assertThat(page.getTotalElements()).isZero();
    assertThat(page.getContent()).isEmpty();
  }

  @Test
  void shouldListAllConfiguredStockTypesInConsolidatedBalanceEvenWithoutMovement() {
    Long tenantId = 108L;
    Long empresaId = createEmpresa(tenantId, "10800000000001");
    var scope = setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");
    UUID tenantUnitId = createTenantUnit(tenantId);
    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaId);

    CatalogItemResponse created = productService.create(new CatalogItemRequest(null, "ITEM ESTOQUE", null, null, tenantUnitId, null, null, true));

    CatalogStockType secondaryStockType = new CatalogStockType();
    secondaryStockType.setTenantId(tenantId);
    secondaryStockType.setCatalogConfigurationId(scope.catalogConfigurationId());
    secondaryStockType.setAgrupadorEmpresaId(scope.agrupadorId());
    secondaryStockType.setCodigo("RESERVA");
    secondaryStockType.setNome("Estoque Reserva");
    secondaryStockType.setOrdem(2);
    secondaryStockType.setActive(true);
    stockTypeRepository.saveAndFlush(secondaryStockType);

    var view = stockQueryService.loadBalanceView(
      CatalogConfigurationType.PRODUCTS,
      created.id(),
      null,
      null,
      null);

    assertThat(view.consolidado())
      .extracting(row -> row.estoqueTipoCodigo())
      .containsExactly("GERAL", "RESERVA");
    assertThat(view.consolidado()).allSatisfy(row -> {
      assertThat(row.quantidadeTotal()).isNotNull();
      assertThat(row.precoTotal()).isNotNull();
      assertThat(row.quantidadeTotal().compareTo(BigDecimal.ZERO)).isZero();
      assertThat(row.precoTotal().compareTo(BigDecimal.ZERO)).isZero();
    });
  }

  @Test
  void shouldListGroupedCompaniesInDetailEvenWithoutMovement() {
    Long tenantId = 109L;
    Long empresaPrincipalId = createEmpresa(tenantId, "10900000000001");
    var scope = setupCatalogGroupLink(tenantId, empresaPrincipalId, CatalogConfigurationType.PRODUCTS, "Grupo Base");
    UUID tenantUnitId = createTenantUnit(tenantId);
    Long empresaSecundariaId = createEmpresa(tenantId, "10900000000002");
    linkEmpresaToCatalogGroup(tenantId, scope.catalogConfigurationId(), scope.agrupadorId(), empresaSecundariaId);

    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaPrincipalId);
    CatalogItemResponse created = productService.create(new CatalogItemRequest(null, "ITEM DETALHAMENTO", null, null, tenantUnitId, null, null, true));

    var view = stockQueryService.loadBalanceView(
      CatalogConfigurationType.PRODUCTS,
      created.id(),
      null,
      null,
      null);

    assertThat(view.rows()).hasSize(2);
    assertThat(view.rows()).extracting(row -> row.filialId())
      .containsExactlyInAnyOrder(empresaPrincipalId, empresaSecundariaId);
    assertThat(view.rows()).extracting(row -> row.filialNome())
      .contains("Empresa 10900000000001", "Empresa 10900000000002");
    assertThat(view.rows()).allSatisfy(row -> {
      assertThat(row.quantidadeAtual()).isNotNull();
      assertThat(row.precoAtual()).isNotNull();
      assertThat(row.quantidadeAtual().compareTo(BigDecimal.ZERO)).isZero();
      assertThat(row.precoAtual().compareTo(BigDecimal.ZERO)).isZero();
    });
  }

  @Test
  void shouldApplyLedgerFiltersIsolatedAndCombined() {
    Long tenantId = 111L;
    Long empresaId = createEmpresa(tenantId, "11100000000001");
    var scope = setupCatalogGroupLink(tenantId, empresaId, CatalogConfigurationType.PRODUCTS, "Grupo Base");
    UUID tenantUnitId = createTenantUnit(tenantId);
    TenantContext.setTenantId(tenantId);
    EmpresaContext.setEmpresaId(empresaId);

    CatalogItemResponse created = productService.create(new CatalogItemRequest(null, "ITEM FILTROS", null, null, tenantUnitId, null, null, true));
    CatalogStockType stockType = stockTypeRepository
      .findAllByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrueOrderByOrdemAscNomeAsc(
        tenantId,
        scope.catalogConfigurationId(),
        scope.agrupadorId())
      .stream()
      .findFirst()
      .orElseThrow();

    saveMovementWithLine(
      tenantId,
      created.id(),
      scope.catalogConfigurationId(),
      scope.agrupadorId(),
      empresaId,
      CatalogMovementOriginType.MOVIMENTO_ESTOQUE,
      "MV-1001",
      1001L,
      "ENTRADA",
      "user-a",
      CatalogMovementMetricType.QUANTIDADE,
      new BigDecimal("3.000000"));

    saveMovementWithLine(
      tenantId,
      created.id(),
      scope.catalogConfigurationId(),
      scope.agrupadorId(),
      empresaId,
      CatalogMovementOriginType.SYSTEM,
      "SYS-2001",
      null,
      "AJUSTE",
      "user-b",
      CatalogMovementMetricType.PRECO,
      new BigDecimal("9.000000"));

    var byOriginCode = stockQueryService.loadLedger(
      CatalogConfigurationType.PRODUCTS,
      created.id(),
      null,
      null,
      "MV-1001",
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      PageRequest.of(0, 20));
    assertThat(byOriginCode.getTotalElements()).isEqualTo(1);
    assertThat(byOriginCode.getContent().get(0).origemMovimentacaoCodigo()).isEqualTo("MV-1001");

    var byUser = stockQueryService.loadLedger(
      CatalogConfigurationType.PRODUCTS,
      created.id(),
      null,
      null,
      null,
      null,
      null,
      "user-b",
      null,
      null,
      null,
      null,
      null,
      PageRequest.of(0, 20));
    assertThat(byUser.getTotalElements()).isEqualTo(1);
    assertThat(byUser.getContent().get(0).origemMovimentacaoCodigo()).isEqualTo("SYS-2001");

    var combined = stockQueryService.loadLedger(
      CatalogConfigurationType.PRODUCTS,
      created.id(),
      null,
      CatalogMovementOriginType.MOVIMENTO_ESTOQUE,
      "MV",
      1001L,
      "ENTRADA",
      "user-a",
      null,
      null,
      CatalogMovementMetricType.QUANTIDADE,
      stockType.getId(),
      empresaId,
      PageRequest.of(0, 20));
    assertThat(combined.getTotalElements()).isEqualTo(1);
    assertThat(combined.getContent().get(0).origemMovimentacaoId()).isEqualTo(1001L);
    assertThat(combined.getContent().get(0).lines())
      .extracting(line -> line.metricType())
      .containsOnly(CatalogMovementMetricType.QUANTIDADE);
  }

  private Long createProductWithSync(
      Long tenantId,
      Long empresaId,
      UUID tenantUnitId,
      CountDownLatch ready,
      CountDownLatch start,
      String nome) throws Exception {
    try {
      TenantContext.setTenantId(tenantId);
      EmpresaContext.setEmpresaId(empresaId);
      ready.countDown();
      start.await(2, TimeUnit.SECONDS);
      CatalogItemResponse created = productService.create(new CatalogItemRequest(null, nome, null, null, tenantUnitId, null, null, true));
      return created.codigo();
    } finally {
      EmpresaContext.clear();
      TenantContext.clear();
    }
  }

  private void saveMovementWithLine(
      Long tenantId,
      Long catalogItemId,
      Long catalogConfigurationId,
      Long agrupadorId,
      Long filialId,
      CatalogMovementOriginType originType,
      String originCode,
      Long originId,
      String movimentoTipo,
      String username,
      CatalogMovementMetricType metricType,
      BigDecimal delta) {
    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken(username, "n/a", java.util.List.of()));
    try {
      CatalogMovement movement = new CatalogMovement();
      movement.setTenantId(tenantId);
      movement.setCatalogType(CatalogConfigurationType.PRODUCTS);
      movement.setCatalogoId(catalogItemId);
      movement.setCatalogConfigurationId(catalogConfigurationId);
      movement.setAgrupadorEmpresaId(agrupadorId);
      movement.setOrigemMovimentacaoTipo(originType);
      movement.setOrigemMovimentacaoCodigo(originCode);
      movement.setOrigemMovimentacaoId(originId);
      movement.setMovimentoTipo(movimentoTipo);
      movement.setOrigemMovimentoItemCodigo("ITEM:" + catalogItemId);
      movement.setIdempotencyKey("test-ledger:" + originCode + ":" + metricType);
      movement.setDataHoraMovimentacao(java.time.Instant.now());
      movement = movementRepository.saveAndFlush(movement);

      CatalogMovementLine line = new CatalogMovementLine();
      line.setTenantId(tenantId);
      line.setMovementId(movement.getId());
      line.setAgrupadorEmpresaId(agrupadorId);
      line.setMetricType(metricType);
      line.setEstoqueTipoId(stockTypeRepository
        .findAllByTenantIdAndCatalogConfigurationIdAndAgrupadorEmpresaIdAndActiveTrueOrderByOrdemAscNomeAsc(
          tenantId,
          catalogConfigurationId,
          agrupadorId)
        .stream()
        .findFirst()
        .orElseThrow()
        .getId());
      line.setFilialId(filialId);
      line.setBeforeValue(BigDecimal.ZERO.setScale(6, java.math.RoundingMode.HALF_UP));
      line.setDelta(delta.setScale(6, java.math.RoundingMode.HALF_UP));
      line.setAfterValue(delta.setScale(6, java.math.RoundingMode.HALF_UP));
      movementLineRepository.saveAndFlush(line);
    } finally {
      SecurityContextHolder.clearContext();
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

  private void linkEmpresaToCatalogGroup(Long tenantId, Long configId, Long agrupadorId, Long empresaId) {
    AgrupadorEmpresa group = agrupadorRepository.findById(agrupadorId).orElseThrow();
    AgrupadorEmpresaItem item = new AgrupadorEmpresaItem();
    item.setTenantId(tenantId);
    item.setConfigType(ConfiguracaoScopeService.TYPE_CATALOGO);
    item.setConfigId(configId);
    item.setAgrupador(group);
    item.setEmpresaId(empresaId);
    agrupadorItemRepository.save(item);
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

  private UUID createTenantUnit(Long tenantId) {
    OfficialUnit officialUnit = officialUnitRepository.findByCodigoOficialIgnoreCase("UN")
      .orElseGet(() -> {
        OfficialUnit unit = new OfficialUnit();
        unit.setCodigoOficial("UN");
        unit.setDescricao("UNIDADE");
        unit.setAtivo(true);
        unit.setOrigem(OfficialUnitOrigin.NFE_TABELA_UNIDADE_COMERCIAL);
        return officialUnitRepository.saveAndFlush(unit);
      });

    return tenantUnitRepository.findByTenantIdAndUnidadeOficialIdAndSystemMirrorTrue(tenantId, officialUnit.getId())
      .map(TenantUnit::getId)
      .orElseGet(() -> {
        TenantUnit unit = new TenantUnit();
        unit.setTenantId(tenantId);
        unit.setUnidadeOficialId(officialUnit.getId());
        unit.setSigla("UN");
        unit.setNome("Unidade");
        unit.setFatorParaOficial(BigDecimal.ONE.setScale(UnitConversionService.FACTOR_SCALE, java.math.RoundingMode.HALF_UP));
        unit.setSystemMirror(true);
        return tenantUnitRepository.saveAndFlush(unit).getId();
      });
  }
}
