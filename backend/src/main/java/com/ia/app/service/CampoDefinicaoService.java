package com.ia.app.service;

import com.ia.app.domain.CampoDefinicao;
import com.ia.app.dto.CampoDefinicaoRequest;
import com.ia.app.repository.CampoDefinicaoRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CampoDefinicaoService {

  private final CampoDefinicaoRepository repository;

  public CampoDefinicaoService(CampoDefinicaoRepository repository) {
    this.repository = repository;
  }

  public Page<CampoDefinicao> list(Long tipoEntidadeId, Pageable pageable) {
    Long tenantId = requireTenant();
    return repository.findAllByTenantIdAndTipoEntidadeId(tenantId, tipoEntidadeId, pageable);
  }

  public CampoDefinicao create(CampoDefinicaoRequest request) {
    Long tenantId = requireTenant();
    CampoDefinicao entity = new CampoDefinicao();
    entity.setTenantId(tenantId);
    entity.setTipoEntidadeId(request.tipoEntidadeId());
    entity.setNome(request.nome());
    entity.setLabel(request.label());
    entity.setTipo(request.tipo());
    entity.setObrigatorio(request.obrigatorio());
    entity.setTamanho(request.tamanho());
    entity.setVersao(1);
    return repository.save(entity);
  }

  public CampoDefinicao update(Long id, CampoDefinicaoRequest request) {
    Long tenantId = requireTenant();
    CampoDefinicao entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("campo_definicao_not_found"));
    entity.setTipoEntidadeId(request.tipoEntidadeId());
    entity.setNome(request.nome());
    entity.setLabel(request.label());
    entity.setTipo(request.tipo());
    entity.setObrigatorio(request.obrigatorio());
    entity.setTamanho(request.tamanho());
    entity.setVersao(entity.getVersao() + 1);
    return repository.save(entity);
  }

  public Instant maxUpdatedAt(Long tipoEntidadeId) {
    Long tenantId = requireTenant();
    return repository.findMaxUpdatedAt(tenantId, tipoEntidadeId);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
