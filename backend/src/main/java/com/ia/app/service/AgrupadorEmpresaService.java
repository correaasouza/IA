package com.ia.app.service;

import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.AgrupadorEmpresaItem;
import com.ia.app.domain.Empresa;
import com.ia.app.dto.AgrupadorEmpresaEmpresaResponse;
import com.ia.app.dto.AgrupadorEmpresaResponse;
import com.ia.app.repository.AgrupadorEmpresaItemRepository;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.tenant.TenantContext;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgrupadorEmpresaService {

  private final AgrupadorEmpresaRepository agrupadorRepository;
  private final AgrupadorEmpresaItemRepository itemRepository;
  private final EmpresaRepository empresaRepository;
  private final ConfiguracaoScopeService configuracaoScopeService;
  private final TipoEntidadeConfigAgrupadorSyncService tipoEntidadeConfigAgrupadorSyncService;
  private final AuditService auditService;
  private final MeterRegistry meterRegistry;

  public AgrupadorEmpresaService(
      AgrupadorEmpresaRepository agrupadorRepository,
      AgrupadorEmpresaItemRepository itemRepository,
      EmpresaRepository empresaRepository,
      ConfiguracaoScopeService configuracaoScopeService,
      TipoEntidadeConfigAgrupadorSyncService tipoEntidadeConfigAgrupadorSyncService,
      AuditService auditService,
      MeterRegistry meterRegistry) {
    this.agrupadorRepository = agrupadorRepository;
    this.itemRepository = itemRepository;
    this.empresaRepository = empresaRepository;
    this.configuracaoScopeService = configuracaoScopeService;
    this.tipoEntidadeConfigAgrupadorSyncService = tipoEntidadeConfigAgrupadorSyncService;
    this.auditService = auditService;
    this.meterRegistry = meterRegistry;
  }

  @Transactional(readOnly = true)
  public List<AgrupadorEmpresaResponse> listar(String configType, Long configId) {
    Long tenantId = requireTenant();
    String normalizedType = configuracaoScopeService.normalizeAndValidate(tenantId, configType, configId);
    List<AgrupadorEmpresa> agrupadores = agrupadorRepository
      .findAllByTenantIdAndConfigTypeAndConfigIdAndAtivoTrueOrderByNomeAsc(tenantId, normalizedType, configId);
    return toResponseList(tenantId, agrupadores);
  }

  @Transactional(readOnly = true)
  public AgrupadorEmpresaResponse detalhe(String configType, Long configId, Long agrupadorId) {
    Long tenantId = requireTenant();
    String normalizedType = configuracaoScopeService.normalizeAndValidate(tenantId, configType, configId);
    AgrupadorEmpresa agrupador = findAgrupador(tenantId, normalizedType, configId, agrupadorId);
    return toResponseList(tenantId, List.of(agrupador)).get(0);
  }

  @Transactional
  public AgrupadorEmpresaResponse criar(String configType, Long configId, String nome) {
    Long tenantId = requireTenant();
    String normalizedType = configuracaoScopeService.normalizeAndValidate(tenantId, configType, configId);
    String normalizedNome = normalizeNome(nome);
    if (agrupadorRepository.existsByTenantIdAndConfigTypeAndConfigIdAndNomeIgnoreCaseAndAtivoTrue(
      tenantId, normalizedType, configId, normalizedNome)) {
      throw new IllegalArgumentException("agrupador_nome_duplicado");
    }
    AgrupadorEmpresa entity = new AgrupadorEmpresa();
    entity.setTenantId(tenantId);
    entity.setConfigType(normalizedType);
    entity.setConfigId(configId);
    entity.setNome(normalizedNome);
    entity.setAtivo(true);
    try {
      AgrupadorEmpresa saved = agrupadorRepository.save(entity);
      if (ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE.equals(normalizedType)) {
        tipoEntidadeConfigAgrupadorSyncService.onAgrupadorCreated(tenantId, configId, saved.getId());
      }
      metric("create", "success");
      auditService.log(tenantId, "AGRUPADOR_EMPRESA_CRIADO", "agrupador_empresa", String.valueOf(saved.getId()),
        "configType=" + normalizedType + ";configId=" + configId + ";nome=" + saved.getNome());
      return toResponseList(tenantId, List.of(saved)).get(0);
    } catch (DataIntegrityViolationException ex) {
      throw mapIntegrityViolation(ex);
    }
  }

  @Transactional
  public AgrupadorEmpresaResponse renomear(String configType, Long configId, Long agrupadorId, String nome) {
    Long tenantId = requireTenant();
    String normalizedType = configuracaoScopeService.normalizeAndValidate(tenantId, configType, configId);
    String normalizedNome = normalizeNome(nome);
    AgrupadorEmpresa entity = findAgrupador(tenantId, normalizedType, configId, agrupadorId);
    if (agrupadorRepository.existsByTenantIdAndConfigTypeAndConfigIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(
      tenantId, normalizedType, configId, normalizedNome, agrupadorId)) {
      throw new IllegalArgumentException("agrupador_nome_duplicado");
    }
    entity.setNome(normalizedNome);
    try {
      AgrupadorEmpresa saved = agrupadorRepository.save(entity);
      metric("rename", "success");
      auditService.log(tenantId, "AGRUPADOR_EMPRESA_RENOMEADO", "agrupador_empresa", String.valueOf(saved.getId()),
        "configType=" + normalizedType + ";configId=" + configId + ";nome=" + saved.getNome());
      return toResponseList(tenantId, List.of(saved)).get(0);
    } catch (DataIntegrityViolationException ex) {
      throw mapIntegrityViolation(ex);
    }
  }

  @Transactional
  public AgrupadorEmpresaResponse adicionarEmpresa(String configType, Long configId, Long agrupadorId, Long empresaId) {
    Long tenantId = requireTenant();
    String normalizedType = configuracaoScopeService.normalizeAndValidate(tenantId, configType, configId);
    AgrupadorEmpresa agrupador = findAgrupador(tenantId, normalizedType, configId, agrupadorId);
    validateEmpresa(tenantId, empresaId);

    boolean alreadyAdded = itemRepository.findByTenantIdAndConfigTypeAndConfigIdAndAgrupadorIdAndEmpresaId(
      tenantId, normalizedType, configId, agrupadorId, empresaId).isPresent();
    if (alreadyAdded) {
      metric("add_empresa", "noop");
      return toResponseList(tenantId, List.of(agrupador)).get(0);
    }

    AgrupadorEmpresaItem item = new AgrupadorEmpresaItem();
    item.setTenantId(tenantId);
    item.setConfigType(normalizedType);
    item.setConfigId(configId);
    item.setAgrupador(agrupador);
    item.setEmpresaId(empresaId);
    try {
      itemRepository.save(item);
      metric("add_empresa", "success");
      auditService.log(tenantId, "AGRUPADOR_EMPRESA_EMPRESA_ADICIONADA", "agrupador_empresa", String.valueOf(agrupadorId),
        "configType=" + normalizedType + ";configId=" + configId + ";empresaId=" + empresaId);
    } catch (DataIntegrityViolationException ex) {
      throw mapIntegrityViolation(ex);
    }
    AgrupadorEmpresa refreshed = findAgrupador(tenantId, normalizedType, configId, agrupadorId);
    return toResponseList(tenantId, List.of(refreshed)).get(0);
  }

  @Transactional
  public AgrupadorEmpresaResponse removerEmpresa(String configType, Long configId, Long agrupadorId, Long empresaId) {
    Long tenantId = requireTenant();
    String normalizedType = configuracaoScopeService.normalizeAndValidate(tenantId, configType, configId);
    AgrupadorEmpresa agrupador = findAgrupador(tenantId, normalizedType, configId, agrupadorId);
    AgrupadorEmpresaItem removed = itemRepository
      .findByTenantIdAndConfigTypeAndConfigIdAndAgrupadorIdAndEmpresaId(
        tenantId, normalizedType, configId, agrupadorId, empresaId)
      .orElseGet(() -> itemRepository
        .findByTenantIdAndConfigTypeAndConfigIdAndEmpresaId(tenantId, normalizedType, configId, empresaId)
        .orElse(null));

    if (removed != null) {
      if (removed.getAgrupador() != null) {
        removed.getAgrupador().getItens().remove(removed);
      }
      itemRepository.delete(removed);
      itemRepository.flush();
    }
    metric("remove_empresa", "success");
    auditService.log(tenantId, "AGRUPADOR_EMPRESA_EMPRESA_REMOVIDA", "agrupador_empresa", String.valueOf(agrupadorId),
      "configType=" + normalizedType + ";configId=" + configId + ";empresaId=" + empresaId);
    AgrupadorEmpresa refreshed = findAgrupador(tenantId, normalizedType, configId, agrupadorId);
    return toResponseList(tenantId, List.of(refreshed)).get(0);
  }

  @Transactional
  public void removerAgrupador(String configType, Long configId, Long agrupadorId) {
    Long tenantId = requireTenant();
    String normalizedType = configuracaoScopeService.normalizeAndValidate(tenantId, configType, configId);
    AgrupadorEmpresa agrupador = findAgrupador(tenantId, normalizedType, configId, agrupadorId);
    if (ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE.equals(normalizedType)) {
      tipoEntidadeConfigAgrupadorSyncService.onAgrupadorRemoved(tenantId, configId, agrupadorId);
    }
    auditService.log(tenantId, "AGRUPADOR_EMPRESA_EXCLUIDO", "agrupador_empresa", String.valueOf(agrupadorId),
      "configType=" + normalizedType + ";configId=" + configId + ";nome=" + agrupador.getNome());
    metric("delete", "success");
    agrupadorRepository.delete(agrupador);
  }

  private AgrupadorEmpresa findAgrupador(Long tenantId, String configType, Long configId, Long agrupadorId) {
    return agrupadorRepository.findByIdAndTenantIdAndConfigTypeAndConfigIdAndAtivoTrue(
      agrupadorId, tenantId, configType, configId).orElseThrow(() -> new EntityNotFoundException("agrupador_not_found"));
  }

  private void validateEmpresa(Long tenantId, Long empresaId) {
    if (empresaId == null || empresaId <= 0) {
      throw new IllegalArgumentException("empresa_id_invalid");
    }
    if (!empresaRepository.existsByIdAndTenantId(empresaId, tenantId)) {
      throw new EntityNotFoundException("empresa_not_found");
    }
  }

  private RuntimeException mapIntegrityViolation(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
    String normalized = message.toLowerCase();
    if (normalized.contains("ux_agrupador_empresa_item_config_empresa")) {
      metric("add_empresa", "conflict");
      return new IllegalArgumentException("empresa_ja_vinculada_outro_agrupador");
    }
    if (normalized.contains("ux_agrupador_empresa_config_nome")) {
      metric("create_or_rename", "conflict");
      return new IllegalArgumentException("agrupador_nome_duplicado");
    }
    metric("unknown", "error");
    return ex;
  }

  private List<AgrupadorEmpresaResponse> toResponseList(Long tenantId, List<AgrupadorEmpresa> agrupadores) {
    Set<Long> empresaIds = agrupadores.stream()
      .flatMap(a -> a.getItens().stream())
      .map(AgrupadorEmpresaItem::getEmpresaId)
      .collect(Collectors.toSet());
    Map<Long, String> empresaNomeById = new HashMap<>();
    if (!empresaIds.isEmpty()) {
      List<Empresa> empresas = empresaRepository.findAllByTenantIdAndIdIn(tenantId, empresaIds);
      for (Empresa empresa : empresas) {
        String nome = empresa.getNomeFantasia() == null || empresa.getNomeFantasia().isBlank()
          ? empresa.getRazaoSocial()
          : empresa.getNomeFantasia();
        empresaNomeById.put(empresa.getId(), nome);
      }
    }

    List<AgrupadorEmpresaResponse> response = new ArrayList<>();
    for (AgrupadorEmpresa agrupador : agrupadores) {
      List<AgrupadorEmpresaEmpresaResponse> empresas = agrupador.getItens().stream()
        .map(item -> new AgrupadorEmpresaEmpresaResponse(
          item.getEmpresaId(),
          empresaNomeById.getOrDefault(item.getEmpresaId(), "Empresa " + item.getEmpresaId())))
        .sorted(Comparator.comparing(AgrupadorEmpresaEmpresaResponse::nome, String.CASE_INSENSITIVE_ORDER))
        .toList();
      response.add(new AgrupadorEmpresaResponse(
        agrupador.getId(),
        agrupador.getNome(),
        agrupador.isAtivo(),
        empresas));
    }
    return response;
  }

  private String normalizeNome(String nome) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("agrupador_nome_required");
    }
    String value = nome.trim();
    if (value.length() > 120) {
      throw new IllegalArgumentException("agrupador_nome_too_long");
    }
    return value;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private void metric(String action, String status) {
    meterRegistry.counter("agrupador_empresa_operacao_total", "action", action, "status", status).increment();
  }
}
