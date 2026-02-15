package com.ia.app.service;

import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.TipoEntidadeConfigPorAgrupador;
import com.ia.app.dto.TipoEntidadeConfigAgrupadorResponse;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.TipoEntidadeConfigPorAgrupadorRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TipoEntidadeConfigPorAgrupadorService {

  private final TipoEntidadeConfigPorAgrupadorRepository repository;
  private final TipoEntidadeRepository tipoEntidadeRepository;
  private final AgrupadorEmpresaRepository agrupadorEmpresaRepository;
  private final TipoEntidadeConfigAgrupadorSyncService syncService;
  private final AuditService auditService;

  public TipoEntidadeConfigPorAgrupadorService(
      TipoEntidadeConfigPorAgrupadorRepository repository,
      TipoEntidadeRepository tipoEntidadeRepository,
      AgrupadorEmpresaRepository agrupadorEmpresaRepository,
      TipoEntidadeConfigAgrupadorSyncService syncService,
      AuditService auditService) {
    this.repository = repository;
    this.tipoEntidadeRepository = tipoEntidadeRepository;
    this.agrupadorEmpresaRepository = agrupadorEmpresaRepository;
    this.syncService = syncService;
    this.auditService = auditService;
  }

  @Transactional
  public List<TipoEntidadeConfigAgrupadorResponse> listar(Long tipoEntidadeId) {
    Long tenantId = requireTenant();
    validateTipoEntidade(tenantId, tipoEntidadeId);
    List<AgrupadorEmpresa> agrupadores = agrupadorEmpresaRepository
      .findAllByTenantIdAndConfigTypeAndConfigIdAndAtivoTrueOrderByNomeAsc(
        tenantId, ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE, tipoEntidadeId);
    for (AgrupadorEmpresa agrupador : agrupadores) {
      syncService.onAgrupadorCreated(tenantId, tipoEntidadeId, agrupador.getId());
    }
    Map<Long, TipoEntidadeConfigPorAgrupador> configByAgrupadorId = new HashMap<>();
    for (TipoEntidadeConfigPorAgrupador config : repository.findAllByTenantIdAndTipoEntidadeIdAndAtivoTrue(tenantId, tipoEntidadeId)) {
      configByAgrupadorId.put(config.getAgrupadorId(), config);
    }
    return agrupadores.stream()
      .map(agrupador -> {
        TipoEntidadeConfigPorAgrupador config = configByAgrupadorId.get(agrupador.getId());
        return new TipoEntidadeConfigAgrupadorResponse(
          agrupador.getId(),
          agrupador.getNome(),
          config != null && config.isObrigarUmTelefone(),
          config != null && config.isAtivo());
      })
      .sorted(Comparator.comparing(TipoEntidadeConfigAgrupadorResponse::agrupadorNome, String.CASE_INSENSITIVE_ORDER))
      .toList();
  }

  @Transactional
  public TipoEntidadeConfigAgrupadorResponse atualizar(Long tipoEntidadeId, Long agrupadorId, boolean obrigarUmTelefone) {
    Long tenantId = requireTenant();
    validateTipoEntidade(tenantId, tipoEntidadeId);
    AgrupadorEmpresa agrupador = agrupadorEmpresaRepository
      .findByIdAndTenantIdAndConfigTypeAndConfigIdAndAtivoTrue(
        agrupadorId, tenantId, ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE, tipoEntidadeId)
      .orElseThrow(() -> new EntityNotFoundException("agrupador_not_found"));

    try {
      TipoEntidadeConfigPorAgrupador config = repository
        .findByTenantIdAndTipoEntidadeIdAndAgrupadorIdAndAtivoTrue(tenantId, tipoEntidadeId, agrupadorId)
        .orElseGet(() -> {
          TipoEntidadeConfigPorAgrupador nova = new TipoEntidadeConfigPorAgrupador();
          nova.setTenantId(tenantId);
          nova.setTipoEntidadeId(tipoEntidadeId);
          nova.setAgrupadorId(agrupadorId);
          nova.setAtivo(true);
          nova.setObrigarUmTelefone(false);
          return nova;
        });
      config.setObrigarUmTelefone(obrigarUmTelefone);
      TipoEntidadeConfigPorAgrupador saved = repository.save(config);
      auditService.log(tenantId, "TIPO_ENTIDADE_CFG_AGRUPADOR_ATUALIZADA", "tipo_entidade_config_agrupador",
        String.valueOf(saved.getId()),
        "tipoEntidadeId=" + tipoEntidadeId + ";agrupadorId=" + agrupadorId + ";obrigarUmTelefone=" + obrigarUmTelefone);
      return new TipoEntidadeConfigAgrupadorResponse(
        agrupador.getId(),
        agrupador.getNome(),
        saved.isObrigarUmTelefone(),
        saved.isAtivo());
    } catch (DataIntegrityViolationException ex) {
      throw mapIntegrityViolation(ex);
    }
  }

  private void validateTipoEntidade(Long tenantId, Long tipoEntidadeId) {
    if (tipoEntidadeId == null || tipoEntidadeId <= 0) {
      throw new IllegalArgumentException("tipo_entidade_id_invalid");
    }
    if (!tipoEntidadeRepository.existsByIdAndTenantIdAndAtivoTrue(tipoEntidadeId, tenantId)) {
      throw new EntityNotFoundException("tipo_entidade_not_found");
    }
  }

  private RuntimeException mapIntegrityViolation(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
    if (message.toLowerCase().contains("ux_tipo_ent_cfg_agrupador_ativo")) {
      return new IllegalArgumentException("tipo_entidade_config_duplicada_agrupador");
    }
    return ex;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
