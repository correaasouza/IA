package com.ia.app.service;

import com.ia.app.domain.TipoEntidade;
import com.ia.app.dto.TipoEntidadeRequest;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TipoEntidadeService {

  private final TipoEntidadeRepository repository;

  public TipoEntidadeService(TipoEntidadeRepository repository) {
    this.repository = repository;
  }

  public Page<TipoEntidade> list(Pageable pageable) {
    Long tenantId = requireTenant();
    return repository.findAllByTenantId(tenantId, pageable);
  }

  public TipoEntidade create(TipoEntidadeRequest request) {
    Long tenantId = requireTenant();
    TipoEntidade entity = new TipoEntidade();
    entity.setTenantId(tenantId);
    entity.setNome(request.nome());
    entity.setVersao(1);
    return repository.save(entity);
  }

  public TipoEntidade update(Long id, TipoEntidadeRequest request) {
    Long tenantId = requireTenant();
    TipoEntidade entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("tipo_entidade_not_found"));
    entity.setNome(request.nome());
    entity.setVersao(entity.getVersao() + 1);
    return repository.save(entity);
  }

  public Instant maxUpdatedAt() {
    Long tenantId = requireTenant();
    return repository.findMaxUpdatedAt(tenantId);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
