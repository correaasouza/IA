package com.ia.app.service;

import com.ia.app.domain.MovimentoConfig;
import com.ia.app.domain.MovimentoConfig.MovimentoConfigItemTipoInput;
import com.ia.app.domain.MovimentoConfigItemTipo;
import com.ia.app.domain.MovimentoItemTipo;
import com.ia.app.dto.MovimentoConfigItemTipoRequest;
import com.ia.app.dto.MovimentoConfigItemTipoResponse;
import com.ia.app.repository.MovimentoConfigRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimentoConfigItemTipoService {

  private final MovimentoConfigRepository movimentoConfigRepository;
  private final MovimentoItemTipoService movimentoItemTipoService;
  private final AuditService auditService;

  public MovimentoConfigItemTipoService(
      MovimentoConfigRepository movimentoConfigRepository,
      MovimentoItemTipoService movimentoItemTipoService,
      AuditService auditService) {
    this.movimentoConfigRepository = movimentoConfigRepository;
    this.movimentoItemTipoService = movimentoItemTipoService;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<MovimentoConfigItemTipoResponse> listByConfig(Long configId) {
    Long tenantId = requireTenant();
    MovimentoConfig config = findConfig(configId, tenantId);
    return toResponseList(config.getTiposItensPermitidos());
  }

  @Transactional
  public List<MovimentoConfigItemTipoResponse> replaceByConfig(Long configId, List<MovimentoConfigItemTipoRequest> request) {
    Long tenantId = requireTenant();
    MovimentoConfig config = findConfig(configId, tenantId);
    List<MovimentoConfigItemTipoInput> inputs = normalizeAndValidateInputs(request);

    config.replaceTiposItensPermitidos(inputs);
    MovimentoConfig saved = movimentoConfigRepository.saveAndFlush(config);
    auditService.log(tenantId,
      "MOVIMENTO_CONFIG_TIPOS_ITENS_ATUALIZADOS",
      "movimento_config",
      String.valueOf(saved.getId()),
      "totalTiposItens=" + saved.getTiposItensPermitidos().size());

    return toResponseList(saved.getTiposItensPermitidos());
  }

  @Transactional(readOnly = true)
  public List<MovimentoConfigItemTipoResponse> listAtivosForConfig(Long configId) {
    Long tenantId = requireTenant();
    MovimentoConfig config = findConfig(configId, tenantId);
    List<MovimentoConfigItemTipoResponse> rows = toResponseList(config.getTiposItensPermitidos());
    return rows.stream().filter(MovimentoConfigItemTipoResponse::ativo).toList();
  }

  private List<MovimentoConfigItemTipoInput> normalizeAndValidateInputs(List<MovimentoConfigItemTipoRequest> request) {
    if (request == null || request.isEmpty()) {
      return List.of();
    }
    Set<Long> ids = new LinkedHashSet<>();
    for (MovimentoConfigItemTipoRequest row : request) {
      if (row == null || row.movimentoItemTipoId() == null || row.movimentoItemTipoId() <= 0) {
        throw new IllegalArgumentException("movimento_config_item_tipo_id_invalid");
      }
      ids.add(row.movimentoItemTipoId());
    }

    for (Long id : ids) {
      movimentoItemTipoService.requireActiveById(id);
    }

    List<MovimentoConfigItemTipoInput> inputs = new ArrayList<>();
    for (Long id : ids) {
      MovimentoConfigItemTipoRequest row = request.stream()
        .filter(item -> item != null && id.equals(item.movimentoItemTipoId()))
        .findFirst()
        .orElseThrow();
      inputs.add(new MovimentoConfigItemTipoInput(id, row.cobrar() == null || row.cobrar()));
    }
    return inputs;
  }

  private List<MovimentoConfigItemTipoResponse> toResponseList(List<MovimentoConfigItemTipo> entities) {
    List<MovimentoConfigItemTipoResponse> result = new ArrayList<>();
    for (MovimentoConfigItemTipo item : entities) {
      MovimentoItemTipo tipo = movimentoItemTipoService.requireById(item.getMovimentoItemTipoId());
      result.add(new MovimentoConfigItemTipoResponse(
        item.getMovimentoItemTipoId(),
        tipo.getNome(),
        tipo.getCatalogType(),
        item.isCobrar(),
        tipo.isAtivo()));
    }
    return result;
  }

  private MovimentoConfig findConfig(Long configId, Long tenantId) {
    if (configId == null || configId <= 0) {
      throw new IllegalArgumentException("movimento_config_id_invalid");
    }
    return movimentoConfigRepository.findByIdAndTenantId(configId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("movimento_config_not_found"));
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
