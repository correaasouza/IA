package com.ia.app.service;

import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.EntidadeFormFieldConfig;
import com.ia.app.domain.EntidadeFormGroupConfig;
import com.ia.app.domain.TipoEntidadeConfigPorAgrupador;
import com.ia.app.dto.EntidadeFormConfigAgrupadorRequest;
import com.ia.app.dto.EntidadeFormConfigAgrupadorResponse;
import com.ia.app.dto.EntidadeFormFieldConfigRequest;
import com.ia.app.dto.EntidadeFormFieldConfigResponse;
import com.ia.app.dto.EntidadeFormGroupConfigRequest;
import com.ia.app.dto.EntidadeFormGroupConfigResponse;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.EntidadeFormFieldConfigRepository;
import com.ia.app.repository.EntidadeFormGroupConfigRepository;
import com.ia.app.repository.TipoEntidadeConfigPorAgrupadorRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntidadeFormConfigService {

  private final TipoEntidadeRepository tipoEntidadeRepository;
  private final AgrupadorEmpresaRepository agrupadorEmpresaRepository;
  private final TipoEntidadeConfigPorAgrupadorRepository tipoEntidadeConfigPorAgrupadorRepository;
  private final EntidadeFormGroupConfigRepository groupConfigRepository;
  private final EntidadeFormFieldConfigRepository fieldConfigRepository;
  private final AuditService auditService;

  public EntidadeFormConfigService(
    TipoEntidadeRepository tipoEntidadeRepository,
    AgrupadorEmpresaRepository agrupadorEmpresaRepository,
    TipoEntidadeConfigPorAgrupadorRepository tipoEntidadeConfigPorAgrupadorRepository,
    EntidadeFormGroupConfigRepository groupConfigRepository,
    EntidadeFormFieldConfigRepository fieldConfigRepository,
    AuditService auditService) {
    this.tipoEntidadeRepository = tipoEntidadeRepository;
    this.agrupadorEmpresaRepository = agrupadorEmpresaRepository;
    this.tipoEntidadeConfigPorAgrupadorRepository = tipoEntidadeConfigPorAgrupadorRepository;
    this.groupConfigRepository = groupConfigRepository;
    this.fieldConfigRepository = fieldConfigRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public EntidadeFormConfigAgrupadorResponse get(Long tipoEntidadeId, Long agrupadorId) {
    Scope scope = resolveScope(tipoEntidadeId, agrupadorId);
    List<EntidadeFormGroupConfigResponse> groups = loadGroups(scope.tenantId, scope.configPorAgrupadorId);
    if (groups.isEmpty()) {
      groups = defaultGroups();
    }
    return new EntidadeFormConfigAgrupadorResponse(
      tipoEntidadeId,
      agrupadorId,
      scope.agrupador.getNome(),
      scope.configPorAgrupador.isObrigarUmTelefone(),
      groups);
  }

  @Transactional
  public EntidadeFormConfigAgrupadorResponse update(
    Long tipoEntidadeId,
    Long agrupadorId,
    EntidadeFormConfigAgrupadorRequest request) {
    Scope scope = resolveScope(tipoEntidadeId, agrupadorId);

    boolean obrigarUmTelefone = request != null && Boolean.TRUE.equals(request.obrigarUmTelefone());
    scope.configPorAgrupador.setObrigarUmTelefone(obrigarUmTelefone);
    tipoEntidadeConfigPorAgrupadorRepository.save(scope.configPorAgrupador);

    List<EntidadeFormGroupConfigRequest> requestedGroups = request == null || request.groups() == null || request.groups().isEmpty()
      ? defaultGroupRequests()
      : request.groups();

    Map<Long, EntidadeFormGroupConfig> persistedGroupsById = new HashMap<>();
    for (EntidadeFormGroupConfigRequest groupRequest : requestedGroups) {
      String groupKey = sanitizeKey(groupRequest.groupKey(), "entidade_form_group_key_required");
      EntidadeFormGroupConfig group = groupConfigRepository
        .findByTenantIdAndTipoEntidadeConfigAgrupadorIdAndGroupKey(scope.tenantId, scope.configPorAgrupadorId, groupKey)
        .orElseGet(() -> {
          EntidadeFormGroupConfig created = new EntidadeFormGroupConfig();
          created.setTenantId(scope.tenantId);
          created.setTipoEntidadeConfigAgrupadorId(scope.configPorAgrupadorId);
          created.setGroupKey(groupKey);
          return created;
        });
      group.setLabel(trimToNull(groupRequest.label()));
      group.setOrdem(normalizeOrder(groupRequest.ordem()));
      group.setEnabled(groupRequest.enabled() == null || groupRequest.enabled());
      group.setCollapsedByDefault(Boolean.TRUE.equals(groupRequest.collapsedByDefault()));
      EntidadeFormGroupConfig savedGroup = groupConfigRepository.save(group);
      persistedGroupsById.put(savedGroup.getId(), savedGroup);

      List<EntidadeFormFieldConfigRequest> requestedFields = groupRequest.fields() == null ? List.of() : groupRequest.fields();
      for (EntidadeFormFieldConfigRequest fieldRequest : requestedFields) {
        String fieldKey = sanitizeKey(fieldRequest.fieldKey(), "entidade_form_field_key_required");
        EntidadeFormFieldConfig field = fieldConfigRepository
          .findByTenantIdAndGroupConfigIdAndFieldKey(scope.tenantId, savedGroup.getId(), fieldKey)
          .orElseGet(() -> {
            EntidadeFormFieldConfig created = new EntidadeFormFieldConfig();
            created.setTenantId(scope.tenantId);
            created.setGroupConfigId(savedGroup.getId());
            created.setFieldKey(fieldKey);
            return created;
          });
        field.setLabel(trimToNull(fieldRequest.label()));
        field.setOrdem(normalizeOrder(fieldRequest.ordem()));
        field.setVisible(fieldRequest.visible() == null || fieldRequest.visible());
        field.setEditable(fieldRequest.editable() == null || fieldRequest.editable());
        field.setRequired(Boolean.TRUE.equals(fieldRequest.required()));
        fieldConfigRepository.save(field);
      }
    }

    auditService.log(
      scope.tenantId,
      "ENTIDADE_FORM_CONFIG_AGRUPADOR_ATUALIZADA",
      "tipo_entidade_config_agrupador",
      String.valueOf(scope.configPorAgrupadorId),
      "tipoEntidadeId=" + tipoEntidadeId + ";agrupadorId=" + agrupadorId + ";obrigarUmTelefone=" + obrigarUmTelefone);

    List<EntidadeFormGroupConfigResponse> groups = loadGroups(scope.tenantId, scope.configPorAgrupadorId);
    return new EntidadeFormConfigAgrupadorResponse(
      tipoEntidadeId,
      agrupadorId,
      scope.agrupador.getNome(),
      scope.configPorAgrupador.isObrigarUmTelefone(),
      groups);
  }

  private List<EntidadeFormGroupConfigResponse> loadGroups(Long tenantId, Long configPorAgrupadorId) {
    List<EntidadeFormGroupConfig> groups = groupConfigRepository
      .findAllByTenantIdAndTipoEntidadeConfigAgrupadorIdOrderByOrdemAscIdAsc(tenantId, configPorAgrupadorId);
    if (groups.isEmpty()) {
      return List.of();
    }
    List<Long> groupIds = groups.stream().map(EntidadeFormGroupConfig::getId).toList();
    Map<Long, List<EntidadeFormFieldConfigResponse>> fieldsByGroupId = new HashMap<>();
    for (EntidadeFormFieldConfig field : fieldConfigRepository.findAllByTenantIdAndGroupConfigIdInOrderByOrdemAscIdAsc(tenantId, groupIds)) {
      fieldsByGroupId
        .computeIfAbsent(field.getGroupConfigId(), k -> new ArrayList<>())
        .add(new EntidadeFormFieldConfigResponse(
          field.getId(),
          field.getFieldKey(),
          field.getLabel(),
          field.getOrdem(),
          field.isVisible(),
          field.isEditable(),
          field.isRequired()));
    }
    return groups.stream()
      .sorted(Comparator.comparing(EntidadeFormGroupConfig::getOrdem).thenComparing(EntidadeFormGroupConfig::getId))
      .map(group -> new EntidadeFormGroupConfigResponse(
        group.getId(),
        group.getGroupKey(),
        group.getLabel(),
        group.getOrdem(),
        group.isEnabled(),
        group.isCollapsedByDefault(),
        fieldsByGroupId.getOrDefault(group.getId(), List.of())))
      .toList();
  }

  private Scope resolveScope(Long tipoEntidadeId, Long agrupadorId) {
    Long tenantId = requireTenant();
    if (tipoEntidadeId == null || tipoEntidadeId <= 0) {
      throw new IllegalArgumentException("tipo_entidade_id_invalid");
    }
    if (!tipoEntidadeRepository.existsByIdAndTenantIdAndAtivoTrue(tipoEntidadeId, tenantId)) {
      throw new EntityNotFoundException("tipo_entidade_not_found");
    }
    AgrupadorEmpresa agrupador = agrupadorEmpresaRepository
      .findByIdAndTenantIdAndConfigTypeAndConfigIdAndAtivoTrue(
        agrupadorId,
        tenantId,
        ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE,
        tipoEntidadeId)
      .orElseThrow(() -> new EntityNotFoundException("agrupador_not_found"));
    TipoEntidadeConfigPorAgrupador configPorAgrupador = tipoEntidadeConfigPorAgrupadorRepository
      .findByTenantIdAndTipoEntidadeIdAndAgrupadorIdAndAtivoTrue(tenantId, tipoEntidadeId, agrupadorId)
      .orElseGet(() -> {
        TipoEntidadeConfigPorAgrupador created = new TipoEntidadeConfigPorAgrupador();
        created.setTenantId(tenantId);
        created.setTipoEntidadeId(tipoEntidadeId);
        created.setAgrupadorId(agrupadorId);
        created.setObrigarUmTelefone(false);
        created.setAtivo(true);
        return tipoEntidadeConfigPorAgrupadorRepository.save(created);
      });
    return new Scope(tenantId, agrupador, configPorAgrupador, configPorAgrupador.getId());
  }

  private String sanitizeKey(String value, String errorKey) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      throw new IllegalArgumentException(errorKey);
    }
    return normalized;
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private Integer normalizeOrder(Integer value) {
    return value == null || value < 0 ? 0 : value;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private record Scope(
    Long tenantId,
    AgrupadorEmpresa agrupador,
    TipoEntidadeConfigPorAgrupador configPorAgrupador,
    Long configPorAgrupadorId
  ) {}

  private List<EntidadeFormGroupConfigResponse> defaultGroups() {
    return defaultGroupRequests().stream()
      .map(group -> new EntidadeFormGroupConfigResponse(
        null,
        group.groupKey(),
        group.label(),
        group.ordem() == null ? 0 : group.ordem(),
        group.enabled() == null || group.enabled(),
        group.collapsedByDefault() != null && group.collapsedByDefault(),
        (group.fields() == null ? List.<EntidadeFormFieldConfigRequest>of() : group.fields()).stream()
          .map(field -> new EntidadeFormFieldConfigResponse(
            null,
            field.fieldKey(),
            field.label(),
            field.ordem() == null ? 0 : field.ordem(),
            field.visible() == null || field.visible(),
            field.editable() == null || field.editable(),
            field.required() != null && field.required()))
          .toList()))
      .toList();
  }

  private List<EntidadeFormGroupConfigRequest> defaultGroupRequests() {
    return List.of(
      new EntidadeFormGroupConfigRequest("DADOS_ENTIDADE", "Dados da entidade", 0, true, false, List.of(
        new EntidadeFormFieldConfigRequest("registroFederal", "Registro federal", 0, true, true, true),
        new EntidadeFormFieldConfigRequest("pessoaNome", "Nome da pessoa", 1, true, true, true),
        new EntidadeFormFieldConfigRequest("pessoaApelido", "Apelido", 2, true, true, false)
      )),
      new EntidadeFormGroupConfigRequest("CONTATOS", "Contatos", 1, true, false, List.of(
        new EntidadeFormFieldConfigRequest("contatos.nome", "Nome do contato", 0, true, true, false),
        new EntidadeFormFieldConfigRequest("contatos.cargo", "Cargo", 1, true, true, false),
        new EntidadeFormFieldConfigRequest("contatos.formas.EMAIL", "E-mail", 2, true, true, false),
        new EntidadeFormFieldConfigRequest("contatos.formas.FONE_CELULAR", "Fone celular", 3, true, true, false),
        new EntidadeFormFieldConfigRequest("contatos.formas.FONE_COMERCIAL", "Fone comercial", 4, true, true, false),
        new EntidadeFormFieldConfigRequest("contatos.formas.WHATSAPP", "WhatsApp", 5, true, true, false)
      )),
      new EntidadeFormGroupConfigRequest("ENDERECOS", "Enderecos", 2, true, false, List.of(
        new EntidadeFormFieldConfigRequest("enderecos.cep", "CEP", 0, true, true, false),
        new EntidadeFormFieldConfigRequest("enderecos.uf", "UF", 1, true, true, false),
        new EntidadeFormFieldConfigRequest("enderecos.municipio", "Municipio", 2, true, true, false),
        new EntidadeFormFieldConfigRequest("enderecos.logradouro", "Logradouro", 3, true, true, false)
      )),
      new EntidadeFormGroupConfigRequest("DOCUMENTACAO", "Documentacao", 3, true, true, List.of(
        new EntidadeFormFieldConfigRequest("documentacao.rg", "RG", 0, true, true, false),
        new EntidadeFormFieldConfigRequest("documentacao.cnh", "CNH", 1, true, true, false),
        new EntidadeFormFieldConfigRequest("documentacao.numeroNif", "Numero NIF", 2, true, true, false)
      )),
      new EntidadeFormGroupConfigRequest("COMERCIAL_FISCAL", "Comercial e fiscal", 4, true, true, List.of(
        new EntidadeFormFieldConfigRequest("comercial.faturamentoDiasPrazo", "Dias de prazo", 0, true, true, false),
        new EntidadeFormFieldConfigRequest("comercial.juroTaxaPadrao", "Juro padrao", 1, true, true, false),
        new EntidadeFormFieldConfigRequest("fiscal.manifestarNotaAutomaticamente", "Manifestar nota automaticamente", 2, true, true, false)
      )),
      new EntidadeFormGroupConfigRequest("RH", "Recursos humanos", 5, true, true, List.of(
        new EntidadeFormFieldConfigRequest("rh.contrato.numero", "Numero contrato", 0, true, true, false),
        new EntidadeFormFieldConfigRequest("rh.contrato.admissaoData", "Data admissao", 1, true, true, false),
        new EntidadeFormFieldConfigRequest("rh.info.atividades", "Atividades", 2, true, true, false)
      )),
      new EntidadeFormGroupConfigRequest("FAMILIARES_REFERENCIAS", "Familiares e referencias", 6, true, true, List.of(
        new EntidadeFormFieldConfigRequest("familiares.parentesco", "Parentesco", 0, true, true, false),
        new EntidadeFormFieldConfigRequest("referencias.nome", "Nome da referencia", 1, true, true, false),
        new EntidadeFormFieldConfigRequest("qualificacoes.rhQualificacaoId", "Qualificacao", 2, true, true, false)
      ))
    );
  }
}

