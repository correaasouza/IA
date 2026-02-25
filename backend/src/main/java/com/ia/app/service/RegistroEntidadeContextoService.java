package com.ia.app.service;

import com.ia.app.domain.AgrupadorEmpresa;
import com.ia.app.domain.AgrupadorEmpresaItem;
import com.ia.app.domain.Empresa;
import com.ia.app.domain.TipoEntidadeConfigPorAgrupador;
import com.ia.app.dto.RegistroEntidadeEmpresaContextoResponse;
import com.ia.app.repository.AgrupadorEmpresaItemRepository;
import com.ia.app.repository.AgrupadorEmpresaRepository;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.TipoEntidadeConfigPorAgrupadorRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.tenant.EmpresaContext;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroEntidadeContextoService {

  public record RegistroEntidadeScope(
    Long tenantId,
    Long empresaId,
    Long tipoEntidadeId,
    Long agrupadorId,
    String agrupadorNome,
    Long tipoEntidadeConfigAgrupadorId) {}

  private final TipoEntidadeRepository tipoEntidadeRepository;
  private final EmpresaRepository empresaRepository;
  private final AgrupadorEmpresaItemRepository agrupadorItemRepository;
  private final AgrupadorEmpresaRepository agrupadorRepository;
  private final TipoEntidadeConfigPorAgrupadorRepository configRepository;
  private final TipoEntidadeConfigAgrupadorSyncService syncService;

  public RegistroEntidadeContextoService(
      TipoEntidadeRepository tipoEntidadeRepository,
      EmpresaRepository empresaRepository,
      AgrupadorEmpresaItemRepository agrupadorItemRepository,
      AgrupadorEmpresaRepository agrupadorRepository,
      TipoEntidadeConfigPorAgrupadorRepository configRepository,
      TipoEntidadeConfigAgrupadorSyncService syncService) {
    this.tipoEntidadeRepository = tipoEntidadeRepository;
    this.empresaRepository = empresaRepository;
    this.agrupadorItemRepository = agrupadorItemRepository;
    this.agrupadorRepository = agrupadorRepository;
    this.configRepository = configRepository;
    this.syncService = syncService;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public RegistroEntidadeEmpresaContextoResponse contexto(Long tipoEntidadeId) {
    Long tenantId = requireTenant();
    Long empresaId = requireEmpresaContext();
    validateTipoEntidade(tenantId, tipoEntidadeId);

    Empresa empresa = empresaRepository.findByIdAndTenantId(empresaId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("empresa_context_not_found"));

    var itemOpt = agrupadorItemRepository.findByTenantIdAndConfigTypeAndConfigIdAndEmpresaId(
      tenantId, ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE, tipoEntidadeId, empresaId);

    if (itemOpt.isEmpty()) {
      return new RegistroEntidadeEmpresaContextoResponse(
        empresaId,
        empresa.getRazaoSocial(),
        tipoEntidadeId,
        null,
        null,
        null,
        false,
        "EMPRESA_SEM_GRUPO_NO_TIPO",
        "A empresa selecionada nao esta vinculada a nenhum Grupo de Empresas para este Tipo de Entidade.");
    }

    AgrupadorEmpresaItem item = itemOpt.get();
    AgrupadorEmpresa agrupador = agrupadorRepository
      .findByIdAndTenantIdAndConfigTypeAndConfigIdAndAtivoTrue(
        item.getAgrupador().getId(),
        tenantId,
        ConfiguracaoScopeService.TYPE_TIPO_ENTIDADE,
        tipoEntidadeId)
      .orElseThrow(() -> new EntityNotFoundException("agrupador_not_found"));

    syncService.onAgrupadorCreated(tenantId, tipoEntidadeId, agrupador.getId());
    TipoEntidadeConfigPorAgrupador config = configRepository
      .findByTenantIdAndTipoEntidadeIdAndAgrupadorIdAndAtivoTrue(tenantId, tipoEntidadeId, agrupador.getId())
      .orElseThrow(() -> new EntityNotFoundException("tipo_entidade_config_agrupador_not_found"));

    return new RegistroEntidadeEmpresaContextoResponse(
      empresaId,
      empresa.getRazaoSocial(),
      tipoEntidadeId,
      agrupador.getId(),
      agrupador.getNome(),
      config.getId(),
      true,
      null,
      null);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public RegistroEntidadeScope resolveObrigatorio(Long tipoEntidadeId) {
    RegistroEntidadeEmpresaContextoResponse contexto = contexto(tipoEntidadeId);
    if (!contexto.vinculado()) {
      throw new IllegalArgumentException("empresa_sem_grupo_no_tipo_entidade");
    }
    return new RegistroEntidadeScope(
      requireTenant(),
      contexto.empresaId(),
      tipoEntidadeId,
      contexto.agrupadorId(),
      contexto.agrupadorNome(),
      contexto.tipoEntidadeConfigAgrupadorId());
  }

  private void validateTipoEntidade(Long tenantId, Long tipoEntidadeId) {
    if (tipoEntidadeId == null || tipoEntidadeId <= 0) {
      throw new IllegalArgumentException("tipo_entidade_id_invalid");
    }
    if (!tipoEntidadeRepository.existsByIdAndTenantIdAndAtivoTrue(tipoEntidadeId, tenantId)) {
      throw new EntityNotFoundException("tipo_entidade_not_found");
    }
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private Long requireEmpresaContext() {
    Long empresaId = EmpresaContext.getEmpresaId();
    if (empresaId == null) {
      throw new IllegalStateException("empresa_context_required");
    }
    return empresaId;
  }
}
