package com.ia.app.service;

import com.ia.app.domain.ContatoTipoPorEntidade;
import com.ia.app.dto.ContatoTipoPorEntidadeRequest;
import com.ia.app.repository.ContatoTipoPorEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContatoTipoPorEntidadeService {

  private final ContatoTipoPorEntidadeRepository repository;

  public ContatoTipoPorEntidadeService(ContatoTipoPorEntidadeRepository repository) {
    this.repository = repository;
  }

  public List<ContatoTipoPorEntidade> list(Long entidadeDefinicaoId) {
    Long tenantId = requireTenant();
    return repository.findAllByTenantIdAndEntidadeDefinicaoId(tenantId, entidadeDefinicaoId);
  }

  public ContatoTipoPorEntidade create(ContatoTipoPorEntidadeRequest request) {
    Long tenantId = requireTenant();
    ContatoTipoPorEntidade entity = new ContatoTipoPorEntidade();
    entity.setTenantId(tenantId);
    entity.setEntidadeDefinicaoId(request.entidadeDefinicaoId());
    entity.setContatoTipoId(request.contatoTipoId());
    entity.setObrigatorio(request.obrigatorio());
    entity.setPrincipalUnico(request.principalUnico());
    return repository.save(entity);
  }

  public ContatoTipoPorEntidade update(Long id, ContatoTipoPorEntidadeRequest request) {
    Long tenantId = requireTenant();
    ContatoTipoPorEntidade entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("contato_tipo_entidade_not_found"));
    entity.setEntidadeDefinicaoId(request.entidadeDefinicaoId());
    entity.setContatoTipoId(request.contatoTipoId());
    entity.setObrigatorio(request.obrigatorio());
    entity.setPrincipalUnico(request.principalUnico());
    return repository.save(entity);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
